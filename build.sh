####################
# Performs a release build

FULL_VERSION=0.9.4-tm-5

# resolve links - $0 may be a softlink
PRG="${0}"

while [ -h "${PRG}" ]; do
  ls=`ls -ld "${PRG}"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "${PRG}"`/"$link"
  fi
done

BASEDIR=`dirname ${PRG}`
BASEDIR=`cd ${BASEDIR};pwd`
cd ${BASEDIR}

if [ -z ${THRIFT_HOME} ] ; then
  echo "THRIFT_HOME not set"
  exit 1
fi

if [ ! -f "${THRIFT_HOME}/bin/thrift" ] ; then
  echo "Thrift compiler not found at : ${THRIFT_HOME}/bin"
  exit 1
fi

# pass githash commit values via environment
if [ -z "${FLUME_GIT_HASH}" ] ; then 
  # not in env, load from file
  if [ -f "$BASEDIR/cloudera/flume_git_hash" ] ; then
    FLUME_GIT_HASH=`cat $BASEDIR/cloudera/flume_git_hash`
  else
    # not in file, load from git repo
    FLUME_GIT_HASH=`cd $BASEDIR && git rev-parse HEAD`
    if [ $? = 0 ] ; then 
      # save the hash for future builds
      echo $FLUME_GIT_HASH > $BASEDIR/cloudera/flume_git_hash
    else
      # not in repo, give up and use unknown
      FLUME_GIT_HASH="unknown"
    fi
  fi
else 
  # git hash previously defined by env, save for next build
  echo $FLUME_GIT_HASH > $BASEDIR/cloudera/flume_git_hash
fi

echo
echo "WARNING: DOCUMENTATION IS NOT BEING GENERATED!!!!!"
echo

# Add -Pfull-build to include documentation build
mvn clean package -Dmaven.revision=${FLUME_GIT_HASH} -Dthrift.executable=${THRIFT_HOME}/bin/thrift -DskipTests ${DO_MAVEN_DEPLOY}

rm -rf build
mkdir build
cp flume-distribution/target/flume-distribution-${FULL_VERSION}-bin.tar.gz build/flume-${FULL_VERSION}.tar.gz


