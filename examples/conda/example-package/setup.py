import setuptools
import examplepackage

setuptools.setup(
    name="examplepackage", # Replace with your own username
    version=examplepackage.__version__,
    author=examplepackage.__author__,
    author_email="olena.gerasiomva@gmail.com",
    description="An example poi package",
    long_description="A small example package for the integration test of Artipie",
    long_description_content_type="text/markdown",
    url="https://github.com/artipie/artipie",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3.7",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    python_requires='>=3.5',
)
