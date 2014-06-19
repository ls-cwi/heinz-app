heinz-cytoscape-app 0.1.0
=========================

This is a Cytoscape 3.x app (plugin) for discovery of differentially
expressed modules in interaction networks, based on gene-wise
experiments. It applies the tool [Heinz](https://software.cwi.nl/heinz),
which implements an algorithm that finds provably optimal
subnetworks using a score scalable by a statiscally interpretable
parameter. It runs the Heinz tool via a simple client-server protocol
(https://github.com/melkebir/server-wrapper), so Heinz does not have to
be installed on the computer from which the analysis is performed.

It is currently still in an early stage of development.

Installation
------------

The app is packaged using [Maven](https://maven.apache.org/). If Maven is
installed, the app can be built from source by downloading (or cloning)
the package, opening a terminal in the root directory of the package,
and executing the command `mvn install`. Maven will then create a JAR
file named heinz-cytoscape-app-*version*.jar in the `target/` directory.

Once the app is built, it can be installed into Cytoscape simply by moving or
copying the JAR file into the `$HOME/CytoscapeConfiguration/3/apps/installed/`
directory. It should then appear in the *Apps* menu in Cytoscape.
