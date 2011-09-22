####################
# Performs a rpm build

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

if [ ! -f "${BASEDIR}/../build/flume-${FULL_VERSION}.tar.gz" ] ; then
  echo "Cannot find built file ${BASEDIR}/../build/flume-${FULL_VERSION}.tar.gz"
  exit 1
fi

rm -f flume*.tar.gz
rm -rf flume*

cp ${BASEDIR}/../build/flume-${FULL_VERSION}.tar.gz . 
tar zxf flume*.tar.gz 

rm -rf usr/lib/flume/tm-5/*

cp -r flume*/lib usr/lib/flume/tm-5/
cp -r flume*/scripts usr/lib/flume/tm-5/
cp -r flume*/test-endtoend usr/lib/flume/tm-5/

rpmbuild -bb --target noarch tm-flume.spec

rm -rf flume*.gz
rm -rf flume*

rpm -qpl tm*.rpm
