heinz-cytoscape-app 0.1.0
=========================

This is a Cytoscape 3.x app (plugin) for discovery of subnetwork modules of differentially
expressed genes in interaction networks. It provides an interface to the tool [Heinz](https://software.cwi.nl/heinz),
which implements an algorithm that finds provably optimal
subnetworks using a statistically interpretable gene-wise score. It runs the Heinz tool via a simple client-server protocol
(https://github.com/melkebir/server-wrapper), so Heinz does not have to
be installed on the computer from which the analysis is performed.

It is currently still in an early stage of development.

Authors
-------

- Fedde Schaeffer
- Mohammed El-Kebir
- Gunnar W. Klau

Installation
------------

The app is packaged using [Maven](https://maven.apache.org/). If
Maven is installed, the app can be built from source by 
downloading (or cloning) the package from [the code repository
on GitHub](https://github.com/ls-cwi/heinz-app), opening a
terminal in the root directory of the package, and executing the
command `mvn install`. Maven will then create a JAR file named
heinz-cytoscape-app-*version*.jar in the `target/` directory.

Once the app is built, it can be installed into Cytoscape simply by moving or
copying the JAR file into the `$HOME/CytoscapeConfiguration/3/apps/installed/`
directory. It should then appear in the *Apps* menu in Cytoscape.

Running the server
------------------

The app needs to contact a server to run Heinz. This server can be run locally, requiring Heinz (see https://software.cwi.nl/heinz) and an interpreter for the Python programming language (version 2.x, see https://www.python.org/) to be installed.  The server wrapper can then be downloaded (or cloned) from https://github.com/melkebir/server-wrapper, and run using commands such as the following:

```
mkdir /tmp/heinz
python src/server.py 9000 1 /tmp/heinz /path/to/heinz
```

Where 9000 is the port to accept connections on, 1 is the maximum number
of Heinz runs to accept simultaneously, /tmp/heinz is the directory to
store temporary files in and /path/to/heinz is the path to the binary
executable of Heinz.
