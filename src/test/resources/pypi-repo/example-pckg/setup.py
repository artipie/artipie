import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="artipietestpkg", 
    version="0.0.3",
    author="Artipie User",
    author_email="example@artipie.com",
    description="An example package for the integration test of Artipie",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/artipie/artipie",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 2.7",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    python_requires='>=2.6',
)
