import sys, os

from kernel import GraphKernel
from similarity import Similarity

def check_all(repo_kernel_file, threshold_list, top_k):
	total = 0
	tp_list = [0]*len(threshold_list)
	fp_list = [0]*len(threshold_list)
	tn_list = [0]*len(threshold_list)
	fn_list = [0]*len(threshold_list)
	acc_list = [0]*len(threshold_list)
	# read kernel only once
	sim = Similarity()
	sim.read_graph_kernels(repo_kernel_file)
	with open(repo_kernel_file, 'r') as fi:
		for line in fi:
			line = line.rstrip()
			parts = line.split('\t')
			dot_file = parts[0]
			result_program_list_with_score = sim.find_top_k_similar_graphs(dot_file, 'g', top_k, 3) # num_iter = 3
			path_parts = dot_file.split(os.sep)
			true_prob = path_parts[-4]
			total += 1
			for (i, threshold) in enumerate(threshold_list):
				cr = check_result(true_prob, result_program_list_with_score, threshold)
				if cr=='tp':
					tp_list[i] += 1
				elif cr=='fp':
					fp_list[i] += 1
				elif cr=='fn':
					fn_list[i] += 1
				else:
					tn_list[i] += 1
				acc = check_top_k_result(true_prob, result_program_list_with_score, threshold, top_k)
				acc_list[i] += acc
	return total, tp_list, fp_list, tn_list, fn_list, acc_list

def check_top_k_result(true_prob, result_program_list_with_score, threshold, top_k):
	top_k_found = [x for (x,y) in result_program_list_with_score[1:] if y > threshold]
	return true_prob in [x.split(os.sep)[-4] for x in top_k_found]

def check_result(true_prob, result_program_list_with_score, threshold):
	dot_path, score = result_program_list_with_score[1] # [0] is itself
	parts = dot_path.split(os.sep)
	found_prob = parts[-4]
	if true_prob==found_prob and score>=threshold:
		return 'tp'  # good 
	elif true_prob!=found_prob and score>=threshold:
		return 'fp'  # threshold still got incorrect
	elif true_prob==found_prob and score<threshold:
		return 'fn' # miss
	else:
		return 'tn'

def xfrange(start, stop, step):
	r = []
	s = start
	while s < stop:
		r.append(s)
		s += step
	return r

def main():
	repo_kernel_file = sys.argv[1] # to read
	top_k = int(sys.argv[2])
	threshold_lower = float(sys.argv[3])
	threshold_upper = float(sys.argv[4])
	threshold_step = float(sys.argv[5])
	
	threshold_list = xfrange(threshold_lower, threshold_upper, threshold_step)

	total, tp_list, fp_list, tn_list, fn_list, acc_list = check_all(repo_kernel_file, threshold_list, top_k)

	tpr_list = [float(x)/total for x in tp_list]
	fpr_list = [float(x)/total for x in fp_list]
	tnr_list = [float(x)/total for x in tn_list]
	fnr_list = [float(x)/total for x in fn_list]
	accr_list = [float(x)/total for x in acc_list]
	print 'The dataset has a total of {0} programs'.format(total)
	print 'Threholds:'
	for th in threshold_list:
		print th
	num_points = len(threshold_list)
	print 'True positve rate:'
	for i in range(num_points):
		print tpr_list[i]
	print 'False positve rate:'
	for i in range(num_points):
		print fpr_list[i]
	print 'True negative rate:'
	for i in range(num_points):
		print tnr_list[i]
	print 'False negative rate:'
	for i in range(num_points):
		print fnr_list[i]
	print 'Top k accuracy (minus finding itself):'
	for i in range(num_points):
		print accr_list[i]

if __name__ == '__main__':
	main()