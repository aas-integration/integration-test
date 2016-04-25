import sys, os
import common

WORKING_DIR = os.path.dirname(os.path.realpath(__file__))
corpus_dir = os.path.join(WORKING_DIR, "corpus")
libs_dir = os.path.join(WORKING_DIR, "libs")
dljc = os.path.join(WORKING_DIR, "do-like-javac", "dljc")
dljc_output_dir = "dljc-out"

def get_project_dir(project_name):
  return os.path.join(corpus_dir, project_name)

def clean_project(project_dir):
  with common.cd(project_dir):
    with open(os.path.join(project_dir, "clean_command.txt"), "r") as f:
      clean_command = f.readline().strip().split()
      common.run_cmd(clean_command)

def run_dljc(project_dir):
  with common.cd(project_dir):
    with open(os.path.join(project_dir, "build_command.txt"), "r") as f:
      build_command = f.readline().strip().split()
      dljc_command = [dljc,
                      "-t", "dyntrace",
                      "-o", dljc_output_dir,
                      "--dyntrace-libs", libs_dir,
                      "--"]
      dljc_command.extend(build_command)
      common.run_cmd(dljc_command)

def run_petablox(project_dir):
  with common.cd(project_dir):
    petablox_cmd = ['java',
                    '-cp', os.path.join(libs_dir, 'petablox.jar'),
                    '-Dpetablox.reflect.kind=none',
                    '-Dpetablox.run.analyses=cipa-0cfa-dlog',
                    'petablox.project.Boot']
    common.run_cmd(petablox_cmd)

def add_project_to_corpus(project_dir):
  """ Assumes that the project_dir contains a
  text file named build_command.txt that contains the build command(s) for the
  project in this directory, and a clean_command.txt that will clean the project.
  """

  clean_project(project_dir)

  """ Run dljc """ #All Tim's stuff
  """ Run Randoop to generate test sources """
  """ Compile test sources """
  """ Run daikon.Chicory on tests to create dtrace file"""
  run_dljc(project_dir)

  """ Precompute graph kernels that are independent of ontology stuff"""
  print "TODO" # Wenchao.

  """ run petablox """
  run_petablox(project_dir)


def update_corpus_project(project_dir):
  """ This is triggered when new type annotations are being added either by
  checker_framework or when the frontend has discovered a set of methods that
  establish certain invariants."""

  """ Recompute dot files """  #Martin S
  print "TODO"

  """ Recompute various kernels """  #Wenchao
  print "TODO"


  """ Update petablox """
  print "TODO"


def find_methods_with_property(property):
  """ TODO we need to have sth to find all methods that take a sequence as input
  and return a sequence. Should be done by LB but can be hardcoded by Tim or Martin."""
  pass


def get_dtrace_file_for_project(project):
  if project == "TODO":
    return os.path.join(WORKING_DIR, "inv_check/test.dtrace.gz")

  dtrace_path = os.path.join(corpus_dir, project, dljc_output_dir, "RegressionTestDriver.dtrace.gz")
  if os.path.exists(dtrace_path):
    return dtrace_path
  else:
    return None

def main():
  for dir in os.listdir(corpus_dir):
    project_dir = get_project_dir(dir)
    if os.path.isdir(project_dir):
      print "Analyzing {}".format(dir)
      add_project_to_corpus(project_dir)
      dtrace = get_dtrace_file_for_project(dir)
      if dtrace:
        print "Generated {}".format(dtrace)

if __name__ == '__main__':
  main()
