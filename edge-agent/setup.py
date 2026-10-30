import os
import sys
from setuptools import setup, Extension, find_packages
ring_buffer_ext = Extension(name='src.buffer._ring_buffer', sources=[os.path.join('src', 'buffer', '_ring_buffer.c')], include_dirs=[], extra_compile_args=['-O3', '-Wall', '-Wextra', '-std=c11'] if sys.platform != 'win32' else ['/O2', '/W3'])
setup(name='edge-agent', version='1.0.0', description='Edge Collection Agent for the Distributed Log Aggregator', packages=find_packages(exclude=['tests', 'tests.*']), ext_modules=[ring_buffer_ext], python_requires='>=3.10', install_requires=['pyyaml>=6.0', 'psutil>=5.9.0', 'protobuf>=4.24.0'], entry_points={'console_scripts': ['edge-agent=src.main:main']})
