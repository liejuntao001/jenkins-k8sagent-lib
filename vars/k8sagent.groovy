#!/usr/bin/env groovy
import com.github.liejuntao001.jenkins.MyYaml

def call(Map opts = [:]) {
  // name is in a format of a+b+c, so the content will be added together from resource
  // example: small+pg we will collect the content from base.yaml, small.yaml, pg.yaml

  String name = opts.get('name', 'base')
  String defaultLabel = "${name.replace('+', '_')}-${UUID.randomUUID().toString()}"
  String label = opts.get('label', defaultLabel)
  String cloud = opts.get('cloud', 'kubernetes')
  String nodeSelector = opts.get('selector', '')
  String jnlpImage = opts.get('jnlpImage', '')

  String doxygen_image

  try {
    doxygen_image = "${TEMPLATE_DOXYGEN_IMAGE}"
  } catch (e) {
    //println("TEMPLATE_DOXYGEN_IMAGE not defined")
    doxygen_image = 'hrektts/doxygen:latest'
  }

  Map template_vars = [:]
  template_vars['TEMPLATE_DOXYGEN_IMAGE'] = doxygen_image

  def ret = [:]

  def comps = name.split('\\+|-').toList()

  if (name != 'base') {
    comps = comps.plus(0, 'base')
  }

  def templates = []
  String template
  for (c in comps) {
    template = libraryResource 'podtemplates/' + c + '.yaml'
    template = renderTemplate(template, template_vars)
    templates.add(template)
  }

  if (nodeSelector) {
    def selector = """
spec:
  nodeSelector:
    ${nodeSelector}
"""
    templates.add(selector)
  }

  if (jnlpImage) {
    def baseImage = """
spec:
  containers:
  - name: jnlp
    image: ${jnlpImage}
"""
    templates.add(baseImage)
  }



  def myyaml = new MyYaml()
  def final_template = myyaml.merge(templates)

  ret['cloud'] = cloud
  ret['label'] = label
  ret['yaml'] = final_template

  return ret
}
