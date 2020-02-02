#!/usr/bin/env groovy
import com.github.liejuntao001.jenkins.MyYaml

def call(Map opts = [:]) {
  // name is in a format of a+b+c, so the content will be added together from resource
  // example: small+pg we will collect the content from base.yaml, small.yaml, pg.yaml

  String name = opts.get('name', 'base')
  String defaultLabel = "${name.replace('+', '_')}-${UUID.randomUUID().toString()}"
  String label = opts.get('label', defaultLabel)
  String cloud = opts.get('cloud', 'kubernetes')
  def ret = [:]

  def comps = name.split('\\+|-').toList()

  if (name != 'base') {
    comps = comps.plus(0, 'base')
  }

  def templates = []
  String template
  for (c in comps) {
    template = libraryResource 'podtemplates/' + c + '.yaml'
    templates.add(template)
  }

  def myyaml = new MyYaml()
  def final_template = myyaml.merge(templates)

  ret['cloud'] = cloud
  ret['label'] = label
  ret['yaml'] = final_template

  return ret
}
