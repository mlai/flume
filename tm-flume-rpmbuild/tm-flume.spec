Buildroot: /usr/src/flume/tm-flume-rpmbuild
Name: tm-flume
Version: 0.9.4tm
Release: 5
Summary: Flume 0.9.4-tm-5
License: Apache License v2.0
Group: System/Daemons
Requires: flume >= 0.9.4

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
ln -sf tm-5/lib .
ln -sf tm-5/scripts .
ln -sf tm-5/test-endtoend .

%preun
cd /usr/lib/flume
rm -f lib scripts test-endtoend
mv -f tm-5/old/* .
rm -rf tm-5
