import os
import common

def run_petablox(project):
  with common.cd(common.get_project_dir(project)):
    petablox_cmd = ['java',
                    '-cp', common.jar_path('petablox.jar'),
                    '-Dpetablox.reflect.kind=none',
                    '-Dpetablox.run.analyses=cipa-0cfa-dlog',
                    'petablox.project.Boot']
    common.run_cmd(petablox_cmd)

def add_project_to_corpus(project):
  """ Assumes that the project_dir contains a
  text file named build_command.txt that contains the build command(s) for the
  project in this directory, and a clean_command.txt that will clean the project.
  """
  common.clean_project(project)

  """Run dljc
  Run Randoop to generate test sources
  Compile test sources
  Run daikon.Chicory on tests to create dtrace file
  Precompute graph kernels that are independent of ontology stuff
  """
  common.run_dljc(project,
                  ['dyntrace', 'graphtool'],
                  ['--graph-jar', common.jar_path('prog2dfg.jar'),
                   '--dyntrace-libs', common.LIBS_DIR])

  """ run petablox """
  #run_petablox(project_dir)


def update_corpus_project(project):
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
    return os.path.join(common.WORKING_DIR, 'inv_check/test.dtrace.gz')

  dtrace_path = os.path.join(common.CORPUS_DIR,
                             project,
                             common.DLJC_OUTPUT_DIR,
                             'RegressionTestDriver.dtrace.gz')
  if os.path.exists(dtrace_path):
    return dtrace_path
  else:
    return None

def main():
  for project in common.get_project_list():
    print "Analyzing {}".format(project)
    add_project_to_corpus(project)
    dtrace = get_dtrace_file_for_project(project)
    if dtrace:
      print "Generated {}".format(dtrace)

if __name__ == '__main__':
  main()
