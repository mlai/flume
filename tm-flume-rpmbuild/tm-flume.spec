Buildroot: /usr/src/flume/tm-flume-rpmbuild
Name: tm-flume
Version: 0.9.4tm
Release: 5+1
Summary: Flume 0.9.4-tm-5
License: Apache License v2.0
Group: System/Daemons
Requires: flume >= 0.9.4, tm-hbase >= 0.90tm-5, tm-hadoop >= 0.20tm-5, tm-zookeeper >= 3.4tm-5

%define _rpmdir ./
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.%%{ARCH}.rpm
%define _unpackaged_files_terminate_build 0
%define _use_internal_dependency_generator 0

%description
Flume 0.9.4-tm-5

%files
"/usr/lib/flume/tm-5"


%post
cd /usr/lib/flume
mkdir -p tm-5/old
mv -f lib tm-5/old
mv -f bin tm-5/old
ln -sf tm-5/bin .
ln -sf tm-5/lib .
ln -sf tm-5/scripts .
ln -sf tm-5/test-endtoend .

%preun
cd /usr/lib/flume
rm -f lib bin scripts test-endtoend
mv -f tm-5/old/* .
rm -rf tm-5
