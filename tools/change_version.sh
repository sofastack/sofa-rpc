#!/bin/bash
shellDir=$(cd "$(dirname "$0")"; pwd)

shopt -s expand_aliases
if [ ! -n "$1" ] ;then
	echo "Please enter a version"
 	exit 1	
else
  	echo "The version is $1 !"
fi

if [ `uname` == "Darwin" ] ;then
 	echo "This is OS X"
 	alias sed='sed -i ""'
else
 	echo "This is Linux"
 	alias sed='sed -i'
fi

echo "Change version in root pom.xml ===>"
sed "/<project /,/<name/ s/<version>[^\$].*<\/version>/<version>$1<\/version>/" $shellDir/../pom.xml
sed "s/<rpc.version>.*<\/rpc.version>/<rpc.version>$1<\/rpc.version>/" $shellDir/../pom.xml

echo "Change version in sofa-rpc-all ===>"
sed "/<project /,/<dependencies/ s/<version>[^\$].*<\/version>/<version>$1<\/version>/" $shellDir/../all/pom.xml
sed "s/<rpc.version>.*<\/rpc.version>/<rpc.version>$1<\/rpc.version>/" $shellDir/../all/pom.xml

echo "Change version in sofa-rpc-bom ===>"
sed "/sofa-rpc-bom</,/<dependencies/ s/<version>[^\$].*<\/version>/<version>$1<\/version>/" $shellDir/../bom/pom.xml
sed "s/<rpc.version>.*<\/rpc.version>/<rpc.version>$1<\/rpc.version>/" $shellDir/../bom/pom.xml

cd $shellDir/..
echo "Change version in subproject pom ===>"
for filename in `find . -name "pom.xml" -mindepth 2`;do
  if [ $filename == "./bom/pom.xml" ]; then
     continue
  elif [ $filename == "./all/pom.xml" ]; then
     continue
  fi
	echo "Deal with $filename"
	sed "/<parent>/,/<dependencies>/ s/<version>[^\$].*<\/version>/<version>$1<\/version>/" $filename
done

# echo "Change version in MANIFEST.MF"
# echo "Deal with MANIFEST.MF"
# sed "s/Plugin-Version.*/Plugin-Version: $1.common/g" $shellDir/../sofa-rpc-plugin/src/main/resources/META-INF/MANIFEST.MF

version_java_file="$shellDir/../core/api/src/main/java/com/alipay/sofa/rpc/common/Version.java"
if [ -f $version_java_file ]; then

    echo "=================="
    echo " WARN: You need to modify \"core/api/src/main/java/com/alipay/sofa/rpc/common/Version.java\" "
    echo "=================="

    version_str=$(echo $1 | \sed -e "s/[^0-9\.]//g")
    echo "VERSION is ${version_str}"
    sed "s/\(VERSION[ ]*=[ ]*\"\).*$/\1${version_str}\";/g" $version_java_file
    
    # number and dot -> padding bugfix version -> padding minor version -> padding major version -> only leave number
    rpc_version=$(echo $1 | \sed -e "s/[^0-9\.]//g" \
        -e "s/\.\([0-9]\)$/\.0\1/" \
        -e "s/\.\([0-9]\)\./\.0\1\./g" \
        -e "s/^\([0-9]\)\./\1\./g" \
        -e "s/[^0-9]//g")
    echo "RPC_VERSION is ${rpc_version}"
    sed "s/\(RPC_VERSION[ ]*=[ ]*\).*$/\1${rpc_version};/g" $version_java_file
    
    date_format=$(date  +"%Y%m%d%H%M%S")
    build_version=${version_str}_${date_format}
    echo "BUILD_VERSION is ${build_version}"
    sed "s/\(BUILD_VERSION[ ]*=[ ]*\"\).*$/\1${build_version}\";/g" $version_java_file
fi
