heinz-cytoscape-app
===================

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

Running the servers
-------------------

The app needs to contact a server to run Heinz. This server can be
run locally, requiring Heinz (see https://software.cwi.nl/heinz) and
an interpreter for the Python programming language (version 2.x, see
https://www.python.org/) to be installed.  The server wrapper can then be
downloaded (or cloned) from https://github.com/melkebir/server-wrapper,
and run using a command such as the following:

```
python src/server.py 9001 1 /path/to/heinz
```

Where 9000 is the port to accept connections on, 1 is the maximum number
of Heinz runs to accept simultaneously, and /path/to/heinz is the path
to the binary executable of Heinz.

To fit BUM models, the app requires another server to be running--possibly
on the same machine, but a different port number. This server uses the
same wrapper script, but instead of Heinz, it should run the R script
`fitBumModel.R` provided in this package. The R script requires an [the
R environment for statistical computing](http://www.r-project.org/) to
be installed, and the `optparse` package within R. Run R in a terminal
and type `install.packages("optparse")` to install it, before quitting
R with `q()`. Then run the following command to test if the script works:

```
Rscript /path/to/fitBumModel.R --help
```

On a *nix machine, the server can then be run with the command:

```
python src/server.py 9000 1 /path/to/fitBumModel.R
```
