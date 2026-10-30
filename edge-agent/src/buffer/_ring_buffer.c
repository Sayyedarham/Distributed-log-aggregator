#define PY_SSIZE_T_CLEAN
#include <Python.h>
#include <pythread.h>

typedef struct {
    PyObject_HEAD
    PyObject **buffer;
    Py_ssize_t capacity;
    Py_ssize_t head;
    Py_ssize_t tail;
    Py_ssize_t count;
    Py_ssize_t dropped_count;
    PyThread_type_lock lock;
} RingBufferObject;

static void
RingBuffer_dealloc(RingBufferObject *self)
{
    if (self->lock) {
        PyThread_acquire_lock(self->lock, NOWAIT_LOCK);
    }
    if (self->buffer) {

        for (Py_ssize_t i = 0; i < self->capacity; i++) {
            Py_XDECREF(self->buffer[i]);
        }
        PyMem_Free(self->buffer);
        self->buffer = NULL;
    }
    if (self->lock) {
        PyThread_release_lock(self->lock);
        PyThread_free_lock(self->lock);
        self->lock = NULL;
    }
    Py_TYPE(self)->tp_free((PyObject *)self);
}

static int
RingBuffer_init(RingBufferObject *self, PyObject *args, PyObject *kwds)
{
    static char *kwlist[] = {"capacity", NULL};
    Py_ssize_t capacity = 10000;

    if (!PyArg_ParseTupleAndKeywords(args, kwds, "|n", kwlist, &capacity)) {
        return -1;
    }

    if (capacity <= 0) {
        PyErr_SetString(PyExc_ValueError, "Capacity must be a positive integer.");
        return -1;
    }

    self->capacity = capacity;
    self->head = 0;
    self->tail = 0;
    self->count = 0;
    self->dropped_count = 0;

    self->buffer = (PyObject **)PyMem_Calloc(capacity, sizeof(PyObject *));
    if (!self->buffer) {
        PyErr_NoMemory();
        return -1;
    }

    self->lock = PyThread_allocate_lock();
    if (!self->lock) {
        PyMem_Free(self->buffer);
        self->buffer = NULL;
        PyErr_SetString(PyExc_RuntimeError, "Failed to allocate thread mutex lock.");
        return -1;
    }

    return 0;
}

static PyObject *
RingBuffer_push(RingBufferObject *self, PyObject *args)
{
    PyObject *item;
    if (!PyArg_ParseTuple(args, "O", &item)) {
        return NULL;
    }

    PyThread_acquire_lock(self->lock, WAIT_LOCK);

    int evicted = 0;
    if (self->count == self->capacity) {

        Py_XDECREF(self->buffer[self->tail]);
        self->buffer[self->tail] = NULL;
        self->tail = (self->tail + 1) % self->capacity;
        self->dropped_count++;
        evicted = 1;
    } else {
        self->count++;
    }

    Py_INCREF(item);
    self->buffer[self->head] = item;
    self->head = (self->head + 1) % self->capacity;

    PyThread_release_lock(self->lock);

    if (evicted) {
        Py_RETURN_TRUE;
    }
    Py_RETURN_FALSE;
}

static PyObject *
RingBuffer_pop(RingBufferObject *self, PyObject *Py_UNUSED(ignored))
{
    PyThread_acquire_lock(self->lock, WAIT_LOCK);

    if (self->count == 0) {
        PyThread_release_lock(self->lock);
        Py_RETURN_NONE;
    }

    PyObject *item = self->buffer[self->tail];
    self->buffer[self->tail] = NULL;
    self->tail = (self->tail + 1) % self->capacity;
    self->count--;

    PyThread_release_lock(self->lock);

    return item;
}

static PyObject *
RingBuffer_pop_batch(RingBufferObject *self, PyObject *args)
{
    Py_ssize_t max_items;
    if (!PyArg_ParseTuple(args, "n", &max_items)) {
        return NULL;
    }

    if (max_items <= 0) {
        return PyList_New(0);
    }

    PyThread_acquire_lock(self->lock, WAIT_LOCK);

    Py_ssize_t to_pop = self->count < max_items ? self->count : max_items;
    PyObject *result = PyList_New(to_pop);
    if (!result) {
        PyThread_release_lock(self->lock);
        return NULL;
    }

    for (Py_ssize_t i = 0; i < to_pop; i++) {
        PyObject *item = self->buffer[self->tail];
        self->buffer[self->tail] = NULL;
        self->tail = (self->tail + 1) % self->capacity;

        PyList_SET_ITEM(result, i, item);
    }
    self->count -= to_pop;

    PyThread_release_lock(self->lock);
    return result;
}

static PyObject *
RingBuffer_peek(RingBufferObject *self, PyObject *Py_UNUSED(ignored))
{
    PyThread_acquire_lock(self->lock, WAIT_LOCK);

    if (self->count == 0) {
        PyThread_release_lock(self->lock);
        Py_RETURN_NONE;
    }

    PyObject *item = self->buffer[self->tail];
    Py_INCREF(item);

    PyThread_release_lock(self->lock);
    return item;
}

static PyObject *
RingBuffer_is_full(RingBufferObject *self, PyObject *Py_UNUSED(ignored))
{
    PyThread_acquire_lock(self->lock, WAIT_LOCK);
    int full = (self->count == self->capacity);
    PyThread_release_lock(self->lock);
    return PyBool_FromLong(full);
}

static PyObject *
RingBuffer_is_empty(RingBufferObject *self, PyObject *Py_UNUSED(ignored))
{
    PyThread_acquire_lock(self->lock, WAIT_LOCK);
    int empty = (self->count == 0);
    PyThread_release_lock(self->lock);
    return PyBool_FromLong(empty);
}

static Py_ssize_t
RingBuffer_len(RingBufferObject *self)
{
    PyThread_acquire_lock(self->lock, WAIT_LOCK);
    Py_ssize_t cnt = self->count;
    PyThread_release_lock(self->lock);
    return cnt;
}

static PyMethodDef RingBuffer_methods[] = {
    {"push", (PyCFunction)RingBuffer_push, METH_VARARGS, "Push an item into the circular buffer with O(1) complexity."},
    {"pop", (PyCFunction)RingBuffer_pop, METH_NOARGS, "Pop the oldest item from the buffer with O(1) complexity."},
    {"pop_batch", (PyCFunction)RingBuffer_pop_batch, METH_VARARGS, "Pop up to N items in FIFO order atomically."},
    {"peek", (PyCFunction)RingBuffer_peek, METH_NOARGS, "Peek at oldest item without removing."},
    {"is_full", (PyCFunction)RingBuffer_is_full, METH_NOARGS, "Return True if buffer is at full capacity."},
    {"is_empty", (PyCFunction)RingBuffer_is_empty, METH_NOARGS, "Return True if buffer is empty."},
    {NULL}
};

static PySequenceMethods RingBuffer_sequence_methods = {
    (lenfunc)RingBuffer_len,
};

static PyTypeObject RingBufferType = {
    PyVarObject_HEAD_INIT(NULL, 0)
    .tp_name = "_ring_buffer.RingBuffer",
    .tp_doc = "High-performance C thread-safe Ring Buffer",
    .tp_basicsize = sizeof(RingBufferObject),
    .tp_itemsize = 0,
    .tp_flags = Py_TPFLAGS_DEFAULT,
    .tp_new = PyType_GenericNew,
    .tp_init = (initproc)RingBuffer_init,
    .tp_dealloc = (destructor)RingBuffer_dealloc,
    .tp_methods = RingBuffer_methods,
    .tp_as_sequence = &RingBuffer_sequence_methods,
};

static PyModuleDef ringbuffermodule = {
    PyModuleDef_HEAD_INIT,
    .m_name = "_ring_buffer",
    .m_doc = "C Extension Module providing O(1) Thread-Safe Ring Buffer.",
    .m_size = -1,
};

PyMODINIT_FUNC
PyInit__ring_buffer(void)
{
    PyObject *m;
    if (PyType_Ready(&RingBufferType) < 0) {
        return NULL;
    }

    m = PyModule_Create(&ringbuffermodule);
    if (m == NULL) {
        return NULL;
    }

    Py_INCREF(&RingBufferType);
    if (PyModule_AddObject(m, "RingBuffer", (PyObject *)&RingBufferType) < 0) {
        Py_DECREF(&RingBufferType);
        Py_DECREF(m);
        return NULL;
    }

    return m;
}

