import sys, os, fnmatch
from kernel import GraphKernel

repo_dir = sys.argv[1]
kernel_file = sys.argv[2] # write to

num_iter = 3 # WL-Kernel iteration number

fo = open(kernel_file, 'w')
for r, ds, fs in os.walk(repo_dir):
	for f in fnmatch.filter(fs, '*.dot'):
		# build graph kerenel
		gk = GraphKernel('g')
		gk.read_dot_graph(os.path.join(r, f))
		gk.init_wl_kernel()
		wls = gk.compute_wl_kernel(num_iter)
		wl_str = "###".join([";;;".join([",,,".join([str(x), str(y)]) for (x,y) in wl]) for wl in wls])
		fo.write(os.path.join(os.path.abspath(os.path.join(r, f)) +'\t' + wl_str + '\t' + str(gk.g.number_of_nodes()) + '\n'))
fo.close()
