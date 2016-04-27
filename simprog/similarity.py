import logging
import itertools
import sys
import multiprocessing as mp
import functools

from kernel import GraphKernel
from vector import *

__author__ = 'Wenchao Li'

class Similarity(object):

	def __init__(self):
		# record the names of the graphs
		self.graphs = []
		# record the classification of the dataset
		self.ylabels = []	
		# need to maintain the wl vectors in the training phase
		self.wl_vectors = {}

	def read_graph_kernels(self, repo_kernel_file):
		with open(repo_kernel_file, 'r') as fi:
			for line in fi:
				newline = line.rstrip()
				line_array = newline.split('\t')
				prog = line_array[0]
				kernel_str = line_array[1]
				gsize = int(line_array[2])
				if gsize==0:
					kernel = []
				else:
					kernel = self.read_kernel_vector_str(kernel_str)
				self.graphs.append(prog)
				self.ylabels.append(0) # just give it the same label; doesn't matter now
				self.wl_vectors[prog] = kernel

	def read_kernel_vector_str(self, kernel_vector_str):
		# return a list of list of tuples: [[(123, 2),(164153, 1)],[...]]
		result = []
		wls_str = kernel_vector_str.split("###")
		for wl_str in wls_str:
			wl_tuple_str = wl_str.split(";;;")
			wl = []
			for tup_str in wl_tuple_str:
				tup_arr = tup_str.split(",,,")
				wl.append((tup_arr[0], int(tup_arr[1])))
			result.append(wl)
		return result

	def read_graph_kernels_old(self, repo_kernel_file):
		with open(repo_kernel_file, 'r') as fi:
			for line in fi:
				newline = line.rstrip()
				line_array = newline.split('\t')
				prog = line_array[0]
				kernel = ast.literal_eval(line_array[1])
				self.graphs.append(prog)
				self.ylabels.append(0) # just give it the same label; doesn't matter now
				self.wl_vectors[prog] = kernel

	def record_graph_feature_using_wl(self, dot_file, name, class_label, num_iter=3, ignore_edge_label=True):
		gk = GraphKernel(name)
		gk.read_dot_graph(dot_file)
		self.graphs.append(name)
		gk.init_wl_kernel()
		wlv = gk.compute_wl_kernel(num_iter)
		self.wl_vectors[name] = wlv
		self.ylabels.append(class_label)

	def compute_wl_kernel_matrix(self, num_iter=3):
		"""
		The kernel matrix is a symmetric matrix containing all pair-wise similarity measures between the graphs.
		"""
		graph_num = len(self.graphs)
		self.kernel_matrix = [[0 for x in range(graph_num)] for x in range(graph_num)]
		for (x,y) in itertools.product(range(graph_num), range(graph_num)):
			if x <= y: # only need upper-triangular portion since the kernel matrix is symmetric
				self.kernel_matrix[x][y] = self.compute_wl_kernel_scalar_product(self.wl_vectors[self.graphs[x]], self.wl_vectors[self.graphs[y]], num_iter)
				self.kernel_matrix[y][x] = self.kernel_matrix[x][y]

	def compute_similarity_between_vectors_old(self, wl1, gs1, wl2, gs2, num_iter, sym=True):
		similarity_score = 0.0
		gs_diff = abs(gs1-gs2)
		self_score1 = self.compute_wl_kernel_scalar_product(wl1, wl1, num_iter)
		#print gs1, self_score1
		if gs1 < gs2:
			similarity_score = float(self.compute_wl_kernel_scalar_product(wl1, wl2, num_iter))*(float(gs1)/(gs2+gs_diff*num_iter))/self_score1
		else:
			if sym: # score(wl1, wl2) = score(wl2, wl1)
				self_score2 = self.compute_wl_kernel_scalar_product(wl2, wl2, num_iter)
				similarity_score = float(self.compute_wl_kernel_scalar_product(wl1, wl2, num_iter))*(float(gs2)/(gs1+gs_diff*num_iter))/self_score2
			else:
				similarity_score = float(elf.compute_wl_kernel_scalar_product(wl1, wl2, num_iter))/self_score1
		return similarity_score

	def compute_similarity_between_vectors(self, wl1, wl2, num_iter):
		if wl1==[] or wl2==[] or wl1[0]==[] or wl2[0]==[]:
			return 0.0
		else:
			wl_kernel = 0.0
			ratio_list = [1.0/(num_iter+1)]*(num_iter+1) # uniform weight for now
			#ratio_list = [0.1, 0.2, 0.3, 0.4]
			#ratio_list = [0.25, 0.25, 0.25, 0.25]
			for i in range(num_iter+1):
				wl_kernel += ratio_list[i]*self.compute_vector_scalar_product(wl1[i], wl2[i])/(self.compute_vector_2norm(wl1[i])*self.compute_vector_2norm(wl2[i]))
			return wl_kernel

	def compute_similarity_between_vectors2(self, wl1, wl2, num_iter):
		return self.compute_wl_angle(wl1, wl2, num_iter)

	def compute_similarity_using_stored_vectors(self, wl, num_iter):
		"""data parallelism"""
		pool = mp.Pool(mp.cpu_count())
		partial_f = functools.partial(compute_similarity_between_vectors, wl1=wl, num_iter=num_iter)
		similarity_vector = pool.map(partial_f, [self.wl_vectors[x] for x in self.graphs])
		pool.close()
		pool.join()
		#graph_num = len(self.graphs)
		#similarity_vector = [0 for x in range(graph_num)]
		#for i in range(graph_num):
		#	similarity_vector[i] = self.compute_similarity_between_vectors(wl, self.wl_vectors[self.graphs[i]], num_iter)
		return similarity_vector

	def compute_test_kernel(self, test_wls, num_iter):
		test_kernel = []
		for (i,wl) in enumerate(test_wls):
			test_kernel.append(self.compute_similarity_using_stored_vectors(wl, num_iter))
		return test_kernel

	def compute_pairwise_similarity(self, k, num_iter):
		for g in self.graphs:
			wl = self.wl_vectors[g]
			similarity_vector = self.compute_similarity_using_stored_vectors(wl, num_iter)
			wl_pairs = list(zip(self.graphs, similarity_vector))
			wl_pairs.sort(key=lambda x: x[1], reverse=True)
			print(g)
			print(wl_pairs[:k])

	def find_top_k_similar_graphs(self, graph_dot_file, graph_name, k, num_iter):
		gk = GraphKernel(graph_name)
		gk.read_dot_graph(graph_dot_file)
		gk.init_wl_kernel()
		wl = gk.compute_wl_kernel(num_iter)
		#graph_size = gk.g.number_of_nodes()
		similarity_vector = self.compute_similarity_using_stored_vectors(wl, num_iter)
		wl_pairs = list(zip(self.graphs, similarity_vector))
		wl_pairs.sort(key=lambda x: x[1], reverse=True)
		if k > len(wl_pairs):
			logging.warning("Trying to select {0} programs out of only {1} programs.".format(k, len(wl_pairs)))
			#return zip(*wl_pairs)[0]
			return wl_pairs[0]
		else:
			#return zip(*wl_pairs)[0][:k]
			return wl_pairs[:k]

	def compute_vector_scalar_product(self, v1, v2):
		"""
		The vectors v1 and v2 do not need to have the same length.
		The scalar product is computed based the Dirac Kernel on the two vectors (in wl, they are histograms).
		"""
		result = 0
		p1 = 0 
		p2 = 0
		while (p1 < len(v1) and p2 < len(v2)):
			if v1[p1][0]<v2[p2][0]:
				p1 += 1
			elif v1[p1][0]>v2[p2][0]:
				p2 += 1
			elif v1[p1][0]==v2[p2][0]:
				result += v1[p1][1]*v2[p2][1]
				p1 += 1
				p2 += 1
		return result

	def compute_vector_2norm(self, v):
		return math.sqrt(sum(x[1]*x[1] for x in v))

	def compute_wl_2norm(self, wl, num_iter):
		s = 0
		for i in range(num_iter+1):
			s += sum([x[1]*x[1] for x in wl[i]])
		return math.sqrt(s)

	def is_empty(self, wl):
		return wl[0]==[]

	def compute_wl_angle(self, wl1, wl2, num_iter):
		if self.is_empty(wl1) or self.is_empty(wl2):
			return 0.0
		else:
			return float(self.compute_wl_kernel_scalar_product(wl1, wl2, num_iter))/(self.compute_wl_2norm(wl1, num_iter)*self.compute_wl_2norm(wl2, num_iter))

	def compute_wl_kernel_scalar_product(self, wl1, wl2, num_iter):
		"""
		Each wl feature vector is a list of lists depending on the number of iterations when running the wl test.
		The parameter num_iter should be less than or equal to len(wl1) and len(wl2), but it is left as an argument to allow the flexibility of choosing a smaller iteration number.
		"""
		result = 0
		# remember each wl*_i is a list of pairs
		for i in range(num_iter+1):
			wl1_i = wl1[i]
			wl2_i = wl2[i]
			result += self.compute_vector_scalar_product(wl1_i, wl2_i)
		return result



