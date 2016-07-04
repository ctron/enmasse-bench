#!/bin/bash
REPONAME=$1
ORG=enmasseproject
REPO="$ORG/$REPONAME"

export TAG=`if [ "$TRAVIS_BRANCH" == "master" ]; then echo "latest"; else echo $TRAVIS_BRANCH ; fi`
echo "TAG: $TAG"
echo "COMMIT: $COMMIT"
echo "TRAVIS_BRANCH: $TRAVIS_BRANCH"
echo "DOCKER_EMAIL: $DOCKER_EMAIL"
echo "REPO: $REPO"

docker build -t $REPO:$COMMIT $REPONAME
if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]
then
    docker login -e $DOCKER_EMAIL -u $DOCKER_USER -p $DOCKER_PASS
    docker tag $REPO:$COMMIT $REPO:$TAG
    docker tag $REPO:$COMMIT $REPO:travis-$TRAVIS_BUILD_NUMBER
    docker push $REPO
fi
