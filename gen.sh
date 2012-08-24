javadoc \
    -outdir $2 \
    -docletpath bin \
    -doclet at.quench.vimdoclet.VimDoclet \
    -sourcepath $1 \
    -subpackages $3
