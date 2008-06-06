import os

from java.io import *

from ucl.physiol.neuroconstruct.cell.examples import *
from ucl.physiol.neuroconstruct.cell.utils import *
from ucl.physiol.neuroconstruct.project import *


#file = File("examples/Ex1-Simple/Ex1-Simple.neuro.xml")
file = File("models/Auditory/Auditory.neuro.xml")

print 'Loading project file: ', file

myProject = Project.loadProject(file,  Project.getDummyProjectEventListener())