@Library('common') import common
def common = new common();

def notifyFailed(reason, error) {
    def commons = new common()
    changelog = commons.getChangeString()
    chatmsg = "**[veilarbaktivitet ${version}](https://itjenester-t1.oera.no/veilarbaktivitet) ${reason} **\n\n${changelog}"
    mattermostSend channel: 'veilarbaktivitet', color: 'danger', message: chatmsg
    throw error
}

node {
    common.setupTools("Maven 3.3.3", "java8")

    stage('Checkout') {
        checkout([$class: 'GitSCM', branches: [[name: "*/${branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [], gitTool: 'Default', submoduleCfg: [], userRemoteConfigs: [[url: 'ssh://git@stash.devillo.no:7999/fo/veilarbaktivitet.git']]])
        pom = readMavenPom file: 'pom.xml'
        if(useSnapshot == 'true') {
            version = pom.version.replace("-SNAPSHOT", ".${currentBuild.number}-SNAPSHOT")
        } else {
            version = pom.version.replace("-SNAPSHOT", ".${currentBuild.number}")
        }
        sh "mvn versions:set -DnewVersion=${version}"
    }

    stage('Build (java)') {
        sh "mvn clean install -P pipeline"
    }

    stage('Run tests (java)') {
        sh "mvn test -P pipeline"
    }

    stage('Deploy nexus') {
        sh "mvn deploy -P pipeline"
        currentBuild.description = "Version: ${version}"
        sh "mvn versions:set -DnewVersion=${pom.version}"
        if(useSnapshot != 'true') {
            sh "git tag -a ${version} -m ${version} HEAD && git push --tags"
        }
    }

    stage('Migrer database') {
        dir('config') {
            sh "mvn process-resources org.flywaydb:flyway-maven-plugin:migrate -Pt6"
        }
    }

    stage('Deploy app') {
        callback = "${env.BUILD_URL}input/Deploy/"

        def author = sh(returnStdout: true, script: 'git --no-pager show -s --format="%an <%ae>" HEAD').trim()
        def deploy = common.deployApp('veilarbaktivitet', version, "${miljo}", callback, author).key

        try {
            timeout(time: 15, unit: 'MINUTES') {
                input id: 'deploy', message: "deployer ${deploy}, deploy OK?"
            }
        } catch(Exception e) {
            msg = 'Deploy feilet [' + deploy + '](https://jira.adeo.no/browse/' + deploy + ')'
            notifyFailed(msg, e)
        }
    }

}

chatmsg = "**[veilarbaktivitet ${version}](https://itjenester-t1.oera.no/veilarbaktivitet) Bygg og deploy OK**\n\n${common.getChangeString()}\n\n Bestill deploy til Q4: navbot deploy veilarbaktivitet ${version} q4"
mattermostSend channel: 'veilarbaktivitet', color: 'good', message: chatmsg