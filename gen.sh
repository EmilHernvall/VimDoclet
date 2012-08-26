javadoc \
    -classpath lib/htmllexer.jar:lib/htmlparser.jar \
    -outdir $2 \
    -docletpath bin:lib/htmllexer.jar:lib/htmlparser.jar \
    -doclet at.quench.vimdoclet.VimDoclet \
    -sourcepath $1 \
    -subpackages $3
