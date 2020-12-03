timestamps {

    properties([
        [$class: 'jenkins.model.BuildDiscarderProperty', strategy: [$class: 'LogRotator',
            artifactDaysToKeepStr: '8',
            artifactNumToKeepStr: '3',
            daysToKeepStr: '15',
            numToKeepStr: '5']
        ]]);

    final def jdks = ['OpenJDK11', 'OpenJDK8']

    node {
        jdks.eachWithIndex { jdk, indexOfJdk ->
            final String jdkTestName = jdk.toString()
            withEnv(["JAVA_HOME=${ tool jdkTestName }", "PATH+MAVEN=${tool 'Maven CURRENT'}/bin:${env.JAVA_HOME}/bin"]) {

                stage('Prepare') {
                    checkout scm
                }

                stage('Build') {
                    echo "Building branch: ${env.BRANCH_NAME}"
                    sh "mvn clean install -B -V -e -fae -q"
                }

                stage('Test') {
                    echo "Running unit tests"
                    sh "mvn -e test -B"
                }

                stage('Integration Test') {
                    echo "Running unit tests"
                    sh "mvn -e verify -B"
                }
                
                if (jdkTestName == 'OpenJDK11') {
                    stage("cleanup Java 11 packages") {
                        echo "Removing Java 11 build artifacts from local repository"
                        sh "mvn clean build-helper:remove-project-artifact"
                    }
                }
            }
        }

        withEnv(["JAVA_HOME=${ tool 'OpenJDK8' }", "PATH+MAVEN=${tool 'Maven CURRENT'}/bin:${env.JAVA_HOME}/bin"]) {

            stage('Publish Test Results') {
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml'
            }

            stage('OWASP Dependency Check') {
                echo "Uitvoeren OWASP dependency check"
                sh "mvn org.owasp:dependency-check-maven:aggregate"
                dependencyCheckPublisher failedNewCritical: 1, unstableNewHigh: 1, unstableNewLow: 1, unstableNewMedium: 1
            }
        }
    }
}
