import os, sys
import logging
from collections import defaultdict

import pip

try:
	import pydotplus
except ImportError:
	pip.main(['install', 'pydotplus'])

try:
	import networkx as nx
except ImportError:
	pip.main(['install', 'networkx==1.10'])
	import networkx as nx

__author__ = 'Wenchao Li'

class TreeKernel(object):
	"""For package dependencies"""

	def __init__(self, name):
		self.name = name


class GraphKernel(object):
	"""For control flow graph, dataflow graph, and call graph from source and compiled code"""

	def __init__(self, name):
		self.name = name
		self.g = None

	def read_dot_graph(self, dot_file):
		logging.info("Read in the dot file and create the corresponding graph.")
		self.dot_file = dot_file
		self.g = nx.DiGraph(nx.read_dot(dot_file))
		#print("graph: "+dot_file)
		#print("number of nodes: "+ str(self.g.number_of_nodes()))
		#print("number of labels: "+ str(len(self.get_all_labels())))

	def get_all_labels(self):
		s = set()
		for n in self.g.nodes():
			s.add(self.g.node[n]['label'])
		return s
		
	def init_wl_kernel(self):
		for n in self.g.nodes():
			if 'label' in self.g.node[n]:
				self.g.node[n]['wl-label0']=str(hash(self.g.node[n]['label']))
			else:
				self.g.node[n]['label'] = 'unlabeled'
				self.g.node[n]['wl-label0']=str(hash(self.g.node[n]['label']))

	def compute_wl_kernel(self, num_iter=3, ignore_edge_label=True):
		logging.info("Compute the Weisfeiler Lehman Graph Kernel.")
		wl_vector = [self.compute_wl_label_base()]
		weight_vector = [1]*num_iter # weigh subgraphs of height h differently
		for i in range(num_iter):
			wl = [(x,y*weight_vector[i]) for (x,y) in self.compute_wl_label(i, ignore_edge_label)]
			wl_vector.append(wl)
		#print self.name, ":", wl_vector
		return wl_vector

	def compute_wl_node_label(self, node, prev_iter, ignore_edge_label=True):
		preds = self.g.predecessors(node)
		succs = self.g.successors(node)
		# label has three parts in the order of pred_label;this_label;succ_label
		pred_labels = ''
		succ_labels = ''
		wl_label_key = 'wl-label'+str(prev_iter)
		if ignore_edge_label:
			pred_labels = [self.g.node[p][wl_label_key] for p in preds]
			succ_labels = [self.g.node[s][wl_label_key] for s in succs]
		else:
			pred_labels = [self.g.node[p][wl_label_key]+','+self.g.edge[p][node]['label'] for p in preds]
			succ_labels = [self.g.node[s][wl_label_key]+','+self.g.edge[node][s]['label'] for s in succs]
		pred_labels.sort()
		succ_labels.sort()
		pred_label = ';'.join(pred_labels)
		succ_label = ';'.join(succ_labels)
		node_label = self.g.node[node][wl_label_key]
		new_label = pred_label + '->' + node_label + '->' + succ_label
		hashed_label = str(hash(new_label))
		# print node, hashed_label
		# store the new node label
		self.g.node[node]['wl-label'+str(prev_iter+1)]=hashed_label
		return hashed_label

	def compute_wl_label_base(self):
		wl_count = defaultdict(int)
		for n in self.g.nodes():
			wl_count[self.g.node[n]['wl-label0']] += 1
		wl_vector = list(wl_count.items())
		wl_vector.sort(key=lambda x: x[0]) # sort lexicographically
		return wl_vector

	def compute_wl_label(self, prev_iter, ignore_edge_label=True):
		wl_count = defaultdict(int)
		for n in self.g.nodes():
			wl_count[self.compute_wl_node_label(n, prev_iter, ignore_edge_label)] += 1
		wl_vector = list(wl_count.items())
		wl_vector.sort(key=lambda x: x[0]) # sort lexicographically
		return wl_vector


