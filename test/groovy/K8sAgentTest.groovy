package groovy

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import com.github.liejuntao001.jenkins.MyYaml

import static junit.framework.Assert.assertEquals

class K8sAgentTest extends BasePipelineTest {
  def agent
  def parser

  def base_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0"""

  def small_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
    resources:
      limits:
        memory: 8Gi
      requests:
        memory: 4Gi
        cpu: 2"""

  def fast_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
    resources:
      limits:
        memory: 31Gi
        cpu: 14
      requests:
        memory: 16Gi
        cpu: 8
  nodeSelector:
    builder: 'fast'"""

  def small_pg_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
    resources:
      limits:
        memory: 8Gi
      requests:
        memory: 4Gi
        cpu: 2
  - name: pg
    image: postgres:9.5.19
    tty: true"""

  def small_pg_sdk25_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
    resources:
      limits:
        memory: 8Gi
      requests:
        memory: 4Gi
        cpu: 2
    env:
    - name: USE_SDK25
      value: true
  - name: pg
    image: postgres:9.5.19
    tty: true"""

  def small_privileged_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
    resources:
      limits:
        memory: 8Gi
      requests:
        memory: 4Gi
        cpu: 2
    securityContext:
      privileged: true
  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: privileged
            operator: In
            values:
            - true
        topologyKey: kubernetes.io/hostname
metadata:
  labels:
    privileged: true"""

  def small_dind_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  - name: dind-storage
    emptyDir: {}
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
    resources:
      limits:
        memory: 8Gi
      requests:
        memory: 4Gi
        cpu: 2
  - name: docker
    image: docker:19.03.3
    command:
    - cat
    tty: true
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
  - name: dind
    image: docker:19.03.3-dind
    securityContext:
      privileged: true
    env:
    - name: DOCKER_TLS_CERTDIR
      value: ''
    args:
    - "--mtu=1440"
    volumeMounts:
      - name: dind-storage
        mountPath: /var/lib/docker"""

  def small_doxygen_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
    resources:
      limits:
        memory: 8Gi
      requests:
        memory: 4Gi
        cpu: 2
  - name: doxygen
    image: hrektts/doxygen:latest
    tty: true"""

  def small_doxygen_external_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
    resources:
      limits:
        memory: 8Gi
      requests:
        memory: 4Gi
        cpu: 2
  - name: doxygen
    image: hrektts/doxygen:my_test_version
    tty: true"""

  def selector_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:3.29-1
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0
  nodeSelector:
    kubernetes.io/hostname: worker1"""

  def jnlpimage_yaml = """spec:
  hostAliases:
  - ip: "192.168.1.15"
    hostnames:
    - "jenkins.example.com"
  volumes:
  - hostPath:
      path: /data/jenkins/repo_mirror
      type: ""
    name: volume-0
  containers:
  - name: jnlp
    image: jenkinsci/jnlp-slave:my_test_version
    imagePullPolicy: Always
    command:
    - /usr/local/bin/jenkins-slave
    volumeMounts:
    - mountPath: /home/jenkins/repo_cache
      name: volume-0"""

  @Before
  void setUp() {
    super.setUp()
    // load
    agent = loadScript("vars/k8sagent.groovy")
    parser = new MyYaml()

    helper.registerAllowedMethod('renderTemplate', [String, Map]) { s,opts ->
      loadScript("vars/renderTemplate.groovy")(s, opts)
    }
  }

  @Test
  void testBase() {
    def expected_yaml = base_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'base')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testSmall() {
    def expected_yaml = small_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'small')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()

  }

  @Test
  void testFast() {
    def expected_yaml = fast_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'fast')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testPg() {
    def expected_yaml = small_pg_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'small+pg')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testSdk25() {
    def expected_yaml = small_pg_sdk25_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'small+pg+sdk25')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testPrivileged() {
    def expected_yaml = small_privileged_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'small+privileged')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testDind() {
    def expected_yaml = small_dind_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'small-dind')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testDoxygen() {
    def expected_yaml = small_doxygen_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'small+doxygen')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testDoxygenExternalVersion() {

    binding.setVariable('TEMPLATE_DOXYGEN_IMAGE', "hrektts/doxygen:my_test_version")

    def expected_yaml = small_doxygen_external_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'small+doxygen')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testNoSelector() {
    def expected_yaml = base_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml
    ]

    def ret = agent(name: 'base')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testSelector() {
    def expected_yaml = selector_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml,
    ]

    def ret = agent(name: 'base', selector: 'kubernetes.io/hostname: worker1')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }

  @Test
  void testJnlpImage() {
    def expected_yaml = jnlpimage_yaml
    def processed_yaml = parser.merge([expected_yaml.toString()])

    def expected = [
        cloud: 'kubernetes',
        yaml : processed_yaml,
    ]

    def ret = agent(name: 'base', jnlpImage: 'jenkinsci/jnlp-slave:my_test_version')

    assertEquals "results", ret.entrySet().containsAll(expected.entrySet()), true
    //assertEquals "results", expected.entrySet(), ret.entrySet()
  }
}
