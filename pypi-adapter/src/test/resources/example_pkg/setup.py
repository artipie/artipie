import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="artipietestpkg", # Replace with your own username
    version="0.0.3",
    author="Artem Lazarev",
    author_email="artem.lazarev@gmail.com",
    description="A small example package for the integration test of Artipie",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/artemlazarev/pypi-adapter",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 2.7",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    python_requires='>=2.6',
)
