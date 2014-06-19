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
