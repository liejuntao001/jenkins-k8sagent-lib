#!/usr/bin/env groovy

def build_on_node = ''
def build_on_cloud = ''
def my_node = [:]

@Library("k8sagent") _

pipeline {
  agent none
  stages {
    stage('init') {
      steps {
        script {
          // Read from job config
          if (!env.BUILD_ON_NODE) {
            error "BUILD_ON_NODE must be defined"
          }
          build_on_node = env.BUILD_ON_NODE

          build_on_cloud = env.BUILD_ON_CLOUD
          if (!build_on_cloud) {
            echo "BUILD_ON_CLOUD not defined, guess as kubernetes"
            build_on_cloud = 'kubernetes'
          }

          // build_on_node could be adjusted dynamically based on conditions
          my_node = k8sagent(name: build_on_node, cloud: build_on_cloud)

          echo "build_on_node is $build_on_node"
          echo "build_on_cloud is $build_on_cloud"
        }
      }
    }

    stage('call other') {
      agent {
        kubernetes(my_node as Map)
      }
      stages {
        stage('pod Demo') {
          steps {
            script {
              sh 'env'
            }
            container('pg') {
              sh 'env'
              sh 'su - postgres -c \'psql --version\''
            }
          }
        }
      }
    }
  }
}
