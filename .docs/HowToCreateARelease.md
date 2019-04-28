# How to create a realease

To create a release you have to checkout the develop branch and than do the following steps
once

    git submodule add $(cat .gitmodules)    
    git submodules init
    git submodules update
    
and the following steps for each release
    
    ./release-scripts/release.sh <new version number> <next version number>
    git push --atomic origin master develop --follow-tags 
    
