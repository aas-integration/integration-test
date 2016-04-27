import os, subprocess, traceback, sys
from contextlib import contextmanager

WORKING_DIR = os.path.dirname(os.path.realpath(__file__))
LIBS_DIR = os.path.join(WORKING_DIR, "libs")
CORPUS_DIR = os.path.join(WORKING_DIR, "corpus")
TOOLS_DIR = os.path.join(WORKING_DIR, "tools")

DLJC_BINARY = os.path.join(TOOLS_DIR, "do-like-javac", "dljc")
DLJC_OUTPUT_DIR = "dljc-out"

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

def get_method(daikon_out):
  arr1 = daikon_out.split('.')
  arr2 = arr1[1].split(':::')
  method = arr2[0]
  return method

def get_simprog():
  return os.path.join(LIBS_DIR, 'simprog')

def get_jar(jar_name):
  path = os.path.join(LIBS_DIR, jar_name)
  if os.path.isfile(path):
    return path
  else:
    return None

def get_project_dir(project_name):
  return os.path.join(CORPUS_DIR, project_name)

def get_project_list():
  return [project for project in  os.listdir(CORPUS_DIR) if os.path.isdir(get_project_dir(project))]

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
