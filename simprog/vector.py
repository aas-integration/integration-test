"""vector math"""

import math

def compute_similarity_between_vectors(wl2, wl1, num_iter):
	if wl1==[] or wl2==[] or wl1[0]==[] or wl2[0]==[]:
		return 0.0
	else:
		wl_kernel = 0.0
		ratio_list = [1.0/(num_iter+1)]*(num_iter+1) # uniform weight for now
		#ratio_list = [0.1, 0.2, 0.3, 0.4]
		#ratio_list = [0.25, 0.25, 0.25, 0.25]
		for i in range(num_iter+1):
			wl_kernel += ratio_list[i]*compute_vector_scalar_product(wl1[i], wl2[i])/(compute_vector_2norm(wl1[i])*compute_vector_2norm(wl2[i]))
		return wl_kernel

def compute_vector_scalar_product(v1, v2):
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


def compute_vector_2norm(v):
	return math.sqrt(sum(x[1]*x[1] for x in v))