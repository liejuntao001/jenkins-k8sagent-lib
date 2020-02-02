#!/usr/bin/env groovy

@Library("k8sagent") _

my_node = k8sagent(name: 'mini-pg')
podTemplate(my_node) {
  node(my_node.label) {
    sh 'echo hello world'
    container('pg') {
      sh 'su - postgres -c \'psql --version\''
    }
  }
}
