mkdir -p out
cp bibliography.bib out/
cp unsrtnat.bst out/
rubber --into out --pdf main.tex
