
import sys, os, shutil
import subprocess
import traceback
import urllib
import zipfile

WORKING_DIR = os.path.dirname(os.path.realpath(__file__))

sys.path.insert(0, os.path.abspath(os.path.join(WORKING_DIR, '..')))
import common


def create_type_annotation(annotation_name, super_type_names=["OntologyTop"]):
  annotation_file_dir = os.path.join(common.TOOLS_DIR, "generic-type-inference-solver/src/ontology/qual/")
  annotation_file_name = os.path.join(annotation_file_dir, "{}.java".format(annotation_name))

  #first generate the definition of the annotation
  with open(annotation_file_name, 'w') as out_file:
    with open("annotation.java.prototype", 'r') as proto_file:
      for line in proto_file.readlines():
        if "$ANNOTATION_NAME$" in line:
          line = line.replace("$ANNOTATION_NAME$", annotation_name)
        if "$SUPERTYPE_NAMES$" in line:
          snames = []
          for super_type_name in super_type_names:
            snames+=["{}.class".format(super_type_name)]
          line = line.replace("$SUPERTYPE_NAMES$", ','.join(snames))
        out_file.write(line)  

def update_ontology_utils(annotation_name, java_type_names):
  """ updates the file 
  tools/generic-type-inference-solver/src/ontology/util/OntologyUtils.java
  which decides which java types get annotated with which annotation.
  """

  if len(java_type_names)==0:
    print ("Can't do empty mapping")
    return

  onotlogy_util_dir = os.path.join(common.TOOLS_DIR, "generic-type-inference-solver/src/ontology/util/")
  onotlogy_util_file = os.path.join(onotlogy_util_dir, "OntologyUtils.java")
  qualified_annotation_name = "ontology.qual.{}".format(annotation_name)

  with open("lala.java", 'w') as out_file:
    with open(onotlogy_util_file, 'r') as proto_file:
      reading_imports = False
      already_imported = False
      for line in proto_file.readlines():
        if line.startswith("import "):
          reading_imports = True
          if qualified_annotation_name in line:
            already_imported = True

        if reading_imports==True:
          if not line.startswith("import ") and len(line.strip())>0:
            reading_imports = False
            if already_imported==False:
              # Import the type annotation that we want to use.
              out_file.write("import {};\n".format(qualified_annotation_name))

        if "return null;" in line:
          # This is where we add the new mapping
          out_file.write("        if (")
          conds = []
          for java_type in java_type_names:
            if java_type == "[]":
              #special case for handling arrays.
              conds+=["type.getKind().equals(TypeKind.ARRAY))"]
            else:
              conds+=["TypesUtils.isDeclaredOfName(type, \"{}\")".format(java_type)]
          out_file.write(" || ".join(conds))
          out_file.write("){\n")
          out_file.write("            AnnotationMirror SEQ = AnnotationUtils.fromClass(elements, {}.class);\n".format(annotation_name))
          out_file.write("            return SEQ;\n")
          out_file.write("         }\n")

        out_file.write(line)
  
  shutil.copyfile("lala.java", qualified_annotation_name)

def recompile_checker_framework():
  type_infer_tool_dir = os.path.join(common.TOOLS_DIR, "checker-framework-inference")
  with common.cd(type_infer_tool_dir):
    jsr308 = common.TOOLS_DIR
    os.environ['JSR308'] = jsr308
    afu = os.path.join(jsr308, 'annotation-tools', 'annotation-file-utilities')
    os.environ['AFU'] = afu
    os.environ['PATH'] += ':' + os.path.join(afu, 'scripts')
    common.run_cmd(["gradle", "dist", "-i"], print_output=True)
  

def main():
  with common.cd(WORKING_DIR):
    annotation = "Disco"
    create_type_annotation(annotation)
    update_ontology_utils(annotation, ["java.util.Collection", "java.util.LinkedList"])
    recompile_checker_framework()
      

if __name__ == '__main__':
  main()
