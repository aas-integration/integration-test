# Graph and Graph Kernel computation.

This uses our old code from the demo workshop but ultimately will be replaced 
by the graph projection inside LB that Martin B. and Wenchao discussed.

*Input*: Currently, the corpus. Later, a connection to the LB database

*Output*: A file called "kernels.txt" that contains the graph kernels for each procedure plus a mapping to the dot file of that procedure.

The current version generates DFG-like dot files for each entry in a project. The dot files are dumped in a sub-folder "./dot" of each corpus project.

In a second step run.sh calls ./scripts/precompute_kernel.py to pre-compute the WL graph kernels. To that end, it walk over all dot files it finds in the corpus directories, computes a kernel, and writes this kernel (together with a mapping to the dot file) in the file ./kernels.txt