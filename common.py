import os, subprocess, traceback, sys
from contextlib import contextmanager

WORKING_DIR = os.path.dirname(os.path.realpath(__file__))
LIBS_DIR = os.path.join(WORKING_DIR, "libs")
CORPUS_DIR = os.path.join(WORKING_DIR, "corpus")
TOOLS_DIR = os.path.join(WORKING_DIR, "tools")

DLJC_BINARY = os.path.join(TOOLS_DIR, "do-like-javac", "dljc")
DLJC_OUTPUT_DIR = "dljc-out"

SIMPROG_DIR = os.path.join(WORKING_DIR, "simprog")

def run_cmd(cmd, print_output=False):
  output = ""
  if print_output:
    print ("Running %s" % ' '.join(cmd))
  try:
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if print_output:
      for line in iter(process.stdout.readline, b''):
        output = output + line
        sys.stdout.write(line)
        sys.stdout.flush()
      process.stdout.close()
    process.wait()
  except:
    print ('calling {cmd} failed\n{trace}'.format(cmd=' '.join(cmd),trace=traceback.format_exc()))
  return output

@contextmanager
def cd(newdir):
  prevdir = os.getcwd()
  os.chdir(os.path.expanduser(newdir))
  try:
    yield
  finally:
    os.chdir(prevdir)

def get_method_from_daikon_out(daikon_out):
  arr1 = daikon_out.split('.')
  arr2 = arr1[1].split(':::')
  method = arr2[0]
  return method

def find_dot_name(method_name, method_file):
  with open(method_file, "r") as fi:
    for line in fi:
      line = line.rstrip()
      arr = line.split('\t')
      method_sig = arr[0]
      dot_name = arr[1]
      if method_name in method_sig:
        return dot_name
  return None

def get_dot_path(project_name, dot_name):
  return os.path.join(get_project_dir(project_name), DLJC_OUTPUT_DIR, '_target_classes', dot_name)

def get_jar(jar_name):
  path = os.path.join(LIBS_DIR, jar_name)
  if os.path.isfile(path):
    return path
  else:
    return None

def get_method_summary_from_dot_path(dot_path):
  arr = dot_path.split(os.sep)
  dot_name = arr[-1]
  dot_dir = arr[:-1]
  proj_dir = arr[:-3]
  method_arr = dot_dir + ["methods.txt"]
  method_file = os.path.join("/", *method_arr)
  sourceline_arr = dot_dir + ["sourcelines.txt"]
  sourceline_file = os.path.join("/", *sourceline_arr)
  mf = open(method_file, "r")
  sf = open(sourceline_file, "r")
  dot_to_method_dict = {}
  method_to_source_dict = {}
  for line in mf:
    line = line.rstrip()
    arr = line.split('\t')
    method_sig = arr[0]
    dot = arr[1]
    dot_to_method_dict[dot] = method_sig
  mf.close()
  for line in sf:
    line = line.rstrip()
    arr = line.split('\t')
    method_sig = arr[0]
    source_file_name = arr[1]
    method_to_source_dict[method_sig] = source_file_name
  sf.close()

  method_sig = dot_to_method_dict[dot_name]
  source_file = method_to_source_dict[method_sig]
  source_path = os.path.join("/", *(proj_dir + ["src/main", source_file]))
  new_method_sig = method_sig[1:-1]
  sig_arr = new_method_sig.split(' ')
  return source_path+"::"+sig_arr[2]+"::"+sig_arr[1]

def get_project_dir(project_name):
  return os.path.join(CORPUS_DIR, project_name)

def get_kernel_path(project_name):
  return os.path.join(get_project_dir(project_name), DLJC_OUTPUT_DIR, '_target_classes', 'kernel.txt')

def get_method_path(project_name):
  return os.path.join(get_project_dir(project_name), DLJC_OUTPUT_DIR, '_target_classes', 'methods.txt')

def get_project_list():
  return [project for project in  os.listdir(CORPUS_DIR) if os.path.isdir(get_project_dir(project))]

def get_simprog(py_file):
  return os.path.join(SIMPROG_DIR, py_file)

def clean_project(project):
  project_dir = get_project_dir(project)
  with cd(project_dir):
    with open(os.path.join(project_dir, 'clean_command.txt'), 'r') as f:
      clean_command = f.readline().strip().split()
      run_cmd(clean_command)

def run_dljc(project, tools, options=[]):
  project_dir = get_project_dir(project)
  with cd(project_dir):
    with open(os.path.join(project_dir, 'build_command.txt'), 'r') as f:
      build_command = f.readline().strip().split()
      dljc_command = [DLJC_BINARY,
                      '-o', DLJC_OUTPUT_DIR,
                      '-t', ','.join(tools)]
      dljc_command.extend(options)
      dljc_command.append('--')
      dljc_command.extend(build_command)
      run_cmd(dljc_command, print_output=True)


CHECKER_ENV_SETUP = False
def setup_checker_framework_env():
  global CHECKER_ENV_SETUP
  if CHECKER_ENV_SETUP:
    return

  jsr308 = TOOLS_DIR
  os.environ['JSR308'] = jsr308

  afu = os.path.join(jsr308, 'annotation-tools', 'annotation-file-utilities')
  os.environ['AFU'] = afu
  os.environ['PATH'] += ':' + os.path.join(afu, 'scripts')
  CHECKER_ENV_SETUP = True
