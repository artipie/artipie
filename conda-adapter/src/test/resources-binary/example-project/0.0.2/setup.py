import setuptools

setuptools.setup(
    name="example-package", # Replace with your own username
    version="0.0.2",
    author="Artipie team",
    author_email="olena.gerasiomva@gmail.com",
    description="An example poi package",
    long_description="A small example package for the integration test of Artipie",
    long_description_content_type="text/markdown",
    url="https://github.com/artipie/conda-adapter",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3.7",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    python_requires='>=3.5',
)
