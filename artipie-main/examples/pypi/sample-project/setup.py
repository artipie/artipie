"""A setuptools based setup module.

See:
https://packaging.python.org/guides/distributing-packages-using-setuptools/
https://github.com/pypa/sampleproject
"""

from setuptools import setup, find_packages
import pathlib

here = pathlib.Path(__file__).parent.resolve()

# Arguments marked as "Required" below must be included for upload to PyPI.
# Fields marked as "Optional" may be commented out.

setup(
    name='sample_project',
    version='0.1',
    author='Artipie',
    package_dir={'': 'src'},
    packages=find_packages(where='src'),  # Required
    python_requires='>=3.5, <4',
)
