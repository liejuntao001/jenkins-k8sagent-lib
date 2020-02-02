#!/usr/bin/env groovy

@Library("k8sagent") _

pipeline {
  agent {
    kubernetes(k8sagent(name: 'mini-pg'))
  }
  stages {
    stage('demo') {
      steps {
        echo "this is a demo"
        script {
          container('pg') {
            sh 'su - postgres -c \'psql --version\''
          }
        }
      }
    }
  }
}
