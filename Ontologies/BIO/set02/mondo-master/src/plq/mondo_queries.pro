:- use_module(library(semweb/rdf11)).
:- use_module(library(semweb/rdfs)).
:- use_module(library(semweb/rdf_turtle)).
:- use_module(library(obo_metadata/oio)).
:- use_module(library(obo_metadata/iao_metadata)).
%:- use_module(library(rdf_owl/owl)).

:- use_module(library(sparqlprog/emulate_builtins)).
:- use_module(library(index_util)).

:- use_module(library(sparqlprog/dataframe)).

:- rdf_register_ns('MONDO','http://purl.obolibrary.org/obo/MONDO_').
:- rdf_register_ns('HP','http://purl.obolibrary.org/obo/HP_').
:- rdf_register_ns('NCIT','http://purl.obolibrary.org/obo/NCIT_').
:- rdf_register_ns('DOID','http://purl.obolibrary.org/obo/DOID_').
:- rdf_register_ns('MESH','http://purl.obolibrary.org/obo/MESH_').
:- rdf_register_ns('GARD','http://purl.obolibrary.org/obo/GARD_').
:- rdf_register_ns('SCTID','http://purl.obolibrary.org/obo/SCTID_').
:- rdf_register_ns('UMLS','http://linkedlifedata.com/resource/umls/id/').
:- rdf_register_ns('MEDGEN','http://purl.obolibrary.org/obo/MEDGEN_').
:- rdf_register_ns('ONCOTREE','http://purl.obolibrary.org/obo/ONCOTREE_').
:- rdf_register_ns('EFO','http://www.ebi.ac.uk/efo/EFO_').
:- rdf_register_ns('RO','http://www.ebi.ac.uk/efo/RO_').
:- rdf_register_ns('HGNC','http://identifiers.org/hgnc/').
:- rdf_register_ns('ICD10','http://purl.obolibrary.org/obo/ICD10_').
:- rdf_register_ns('Orphanet','http://www.orpha.net/ORDO/Orphanet_').
:- rdf_register_ns(oio,'http://www.geneontology.org/formats/oboInOwl#').

% fake
:- rdf_register_ns('DC','http://purl.obolibrary.org/obo/DC_').
:- rdf_register_ns('COHD','http://purl.obolibrary.org/obo/COHD_').
:- rdf_register_ns('OMIMPS','http://purl.obolibrary.org/obo/OMIMPS_').

foo('0').

ix :-
        materialize_index(owl_edge(_,_,_,_)),
        materialize_index(mondo_equiv_class_via_xref(_,_)),
        materialize_index(xref_prefix(_,_,_)).


call_unique(G) :- setof(G,G,Gs),member(G,Gs).


mondo(C) :- owl:class(C), uri_prefix(C,'MONDO').


% TODO: add to owl vocab
dep(C) :- rdf(C,owl:deprecated,_).

solutions(X,G,L) :-
        setof(X,X^G,L),
        !.
solutions(_,_,[]).

exact_synonym_src(C,S,X) :-
        has_exact_synonym_axiom(C,S,A),
        has_dbxref(A,X).

exact_synonym_srcs(C,S,Xs) :-
        has_exact_synonym(C,S),
        solutions(X,exact_synonym_src(C,S,X),Xs).

exact_synonym_srcs(C,S,['PRIMARY_LABEL']) :-
        label(C,S).

clashing_exact_synonym_srcs(S,C,Xs,C2,Xs2) :-
        exact_synonym_srcs(C,S,Xs),
        exact_synonym_srcs(C2,S,Xs2),
        C2\=C,
        \+ deprecated(C),
        \+ deprecated(C2).

% ./mq report synclash
dataframe:dataframe(synclash,
                    [[syn=S,
                      c1=C,
                      xrefs1=Xs,
                      c2=C2,
                      xrefs2=Xs2]-clashing_exact_synonym_srcs(S,C,Xs,C2,Xs2)],
                    [
                     description('clashing synonyms'),
                     entity(c1),
                     entity(c2)
                    ]).
        

%! xref_prefix(?Cls, ?Xref, ?Prefix:str) is nondet.
xref_prefix(C,X,P) :-
        has_dbxref(C,X),
        curie_ns(X,P).

xref_src(C,X,S) :-
        xref_src(C,X,_,S).
xref_src(C,X,A,S) :-
        has_dbxref_axiom(C,X,A),
        rdf(A,oio:source,S).

xref_prefix_srcont(C,X,P,S) :-
        xref_prefix(C,X,P),
        xref_src(C,X,SC),
        curie_ns(SC,S).


%! curie_ns(XrefLiteral:str, Pre:str)
curie_ns(Literal,Pre) :-
        str_before(Literal,":",Pre).

%!  mondo_equiv_xref(?Cls:atom, ?Xref:str, ?Prefix:str)
mondo_equiv_xref(C,X) :-
        xref_src(C,X,_,"MONDO:equivalentTo").
mondo_equiv_xref(C,X,P) :-
        xref_src(C,X,_,"MONDO:equivalentTo"),
        curie_ns(X,P).


mondo_xref_semantics(C,X,Rel,P) :-
        xref_src(C,X,_,Src),
        curie_ns(X,P),
        src_semantics(Src,Rel).
mondo_xref_semantics(C,X,Rel) :-
        xref_src(C,X,_,Src),
        src_semantics(Src,Rel).


src_semantics("MONDO:equivalentTo",(=)) :- !.
src_semantics("MONDO:subClassOf",(<)) :- !.
src_semantics("MONDO:superClassOf",(>)) :- !.
src_semantics("MONDO:relatedTo",(~)) :- !.
src_semantics(_,(-)) :- !.

no_mondo_equivalent(X.P) :-
        owl:class(X),
        uri_prefix(X,P),
        \+ mondo_equiv_xref(_,X,_).

mondo_equiv_class_via_xref(C,XC,P) :-
        mondo_equiv_xref(C,X,P),
        curie_uri(X,XC).
mondo_equiv_class_via_xref(C,XC) :-
        mondo_equiv_xref(C,X),
        curie_uri(X,XC).

mondo_equiv_xref_dangling(C,X,Rel,P) :-
        mondo_xref_semantics(C,X,Rel,P),
        curie_uri(X,XC),
        \+ owl:class(XC).

tag_mondo_equiv_xref_dangling(P) :-
        mondo_equiv_xref_dangling(C,X,_Rel,P),
        owl_assert_axiom_with_anns(rdf(C,oio:hasDbXref,X),xref,[annotation(oio:source,"MONDO:notFoundInSource")]),
        fail.
tag_mondo_equiv_xref_dangling(_) :-
        rdf_assert(oio:source, rdf:type, owl:'AnnotationProperty', xref),
        rdf_assert(oio:hasDbXref, rdf:type, owl:'AnnotationProperty', xref).

rare_disease('MONDO:0021200').

rare(C) :- rare_disease(C).
rare(C) :- mondo_equiv_xref(C,_,'OMIM').

%! curie_uri(+Literal:literal, ?URI:atom) is semidet
curie_uri(Literal,URI) :-
        Literal = S^^_,
        atom_string(A,S),
        concat_atom([Pre,Post],':',A),
        \+ \+ rdf_current_prefix(Pre,_),
        rdf_global_id(Pre:Post,URI).

% TODO
uri_curie(URI,Literal) :-
        rdf_global_id(Pre:Post,URI),
        concat_atom([Pre,Post],':',A),
        atom_string(A,Literal).

%! uri_prefix(?URI:atom, ?Prefix:atom)
uri_prefix(URI,Prefix) :-
        rdf_global_id(Prefix:_,URI).

mondo_shared_xref(C,X,SharedX,XPre,SharedXPre) :-
        mondo_equiv_xref(C,SharedX,SharedXPre),
        has_dbxref(X,SharedX),
        uri_prefix(X,XPre).


ont_missing_xref(Ont,X) :-
        class(X),
        uri_prefix(X,Ont),
        uri_curie(X,XId),
        \+ has_dbxref(_,XId).

ordo_missing_xref(C,Xs) :-
        class(C),
        rdfs_subclass_of(C,'http://purl.obolibrary.org/obo/Orphanet_C001'),
        uri_curie(C,CId),
        \+ has_dbxref(_,CId),
        solutions(X,has_dbxref(C,X),Xs).

ordo_missing_xref_src(C,X,S) :-
        class(C),
        rdfs_subclass_of(C,'http://purl.obolibrary.org/obo/Orphanet_C001'),
        uri_curie(C,CId),
        \+ has_dbxref(_,CId),
        xref_src(C,X,S).

ext_merged(X1,X2,C) :-
        ext_merged(X1,X2,C,_).
ext_merged(XC1,XC2,C,P) :-
        mondo_equiv_xref(C,X1),
        mondo_equiv_xref(C,X2),
        curie_ns(X1,P),
        curie_ns(X2,P),
        curie_uri(X1,XC1),
        curie_uri(X2,XC2),
        X1 @< X2.

equivalent_to_deprecated(C,X) :-
        owl_equivalent_class(C,X),
        C\=X,
        deprecated(X).

equivalent_to_replaced_by(C,X,Y) :-
        owl_equivalent_class(C,X),
        C\=X,
        deprecated(X),
        term_replaced_by(X,Y).

simj_merge_candidates(C1,C2,S,N1,N2) :-
        simj_by_subclass(C1,C2,S,N1,N2),
        N1 > 3,
        S > 0.8,
        C1 @< C2.


obsoletion_status(C,S) :-
        (   deprecated(C)
        ->  S=deprecated
        ;   \+ label(C,_)
        ->  S=not_present
        ;   S=live).


proxy_merge(C,X1,X2,S,Ob1,Ob2) :-
        owl_equivalent_class(C,X1),
        mondo(C),
        owl_equivalent_class(C,X2),
        X1 @< X2,
        uri_prefix(X1,S),
        uri_prefix(X2,S),
        obsoletion_status(X1,Ob1),
        obsoletion_status(X2,Ob2).


xref_relation_inferred(C,X,R) :-
        has_dbxref(C,X),
        owl:class(C),
        \+ mondo_equiv_xref(C,X),
        mondo_equiv_xref(D,X),
        rel(C,R,D).

mondo2hgnc(M,G) :-
        subclass_of_some(M,_,G),
        uri_prefix(G,'HGNC').

mondo2hgnc_inf(M,G,M) :- mondo2hgnc(M,G).
mondo2hgnc_inf(M,G,Z) :- rdf_path(M,zeroOrMore(rdfs:subClassOf),Z),mondo2hgnc(Z,G).

mondo2hgnc_inf(M,G) :- mondo2hgnc_inf(M,G,_).

mondo2hgnc_conflict(M,G1,G2,Z1,Z2) :- mondo2hgnc_inf(M,G1,Z1),mondo2hgnc_inf(M,G2,Z2),G1 \= G2.
mondo2hgnc_conflict(M,G1,G2) :- mondo2hgnc_conflict(M,G1,G2,_,_).


gene_level_nested(C,D,G) :-
        mondo2hgnc(C,G),
        rdfs_subclass_of(C,D),
        D\=C,
        mondo2hgnc(D,G).


% grep -i susc mim2gene_medgen | perl -npe 's@@OMIM:@' | cut -f1 | tbl2p -p s > susc.pro
% ./mq -f tsv -l -c susc.pro mondo_susc > z
xxxmondo_susc(M) :-
        s(X),
        atom_string(X,Y),
        mondo_equiv_xref(M,Y).


rel(C,subClassOf,D) :- rdfs_subclass_of(C,D), !.
rel(C,superClassOf,D) :- rdfs_subclass_of(D,C), !.
rel(C,equivalentTo,D) :- owl_equivalent_class(C,D), !.
rel(C,directSiblingOf,D) :- subClassOf(C,Z),subClassOf(D,Z), !.
rel(_,relatedTo,_).


% pl2sparql -d index -g ix -A ~/repos/onto-mirror/void.ttl -f tsv -i mondo-edit.owl -c ../plq/mondo_queries.pro  -e -i ordo  disease_modifier_gene -l
disease_modifier_gene(D,Dx,P,Gx,G) :-
        %lmatch("Modifying germline mutation in",P),
        owl_edge(Gx,P,Dx),
        xref_prefix(Gx,G,"HGNC"),
        mondo_equiv_class_via_xref(D,Dx).


dataframe:dataframe(susceptibility,
                    [[class=C]-susc(C),
                     [omim=X]-entity_xref_prefix(C,X,"OMIM"),
                     [orphanet=X]-entity_xref_prefix(C,X,"Orphanet")],
                    [
                     description('Proposed obsoletions for susceptibility terms')
                    ]).
susc(X) :-
        tsearch('susceptibility to',X).

unique_isa(C,D,Prefix,CXs,DXs) :-
        owl:subClassOf_axiom(C,D,A),
        rdf(A,oio:source,S),
        curie_ns(S,Prefix),
        \+ ((rdf(A,oio:source,S2),
             S2\=S)),
        solutions(X,mondo_equiv_class_via_xref(C,X),CXs),
        solutions(X,mondo_equiv_class_via_xref(D,X),DXs).

dataframe:dataframe(unique_isa,
                    [[child=C,
                      parent=D,
                      source=Prefix]-(
                                      owl:subClassOf_axiom(C,D,A),
                                      rdf(A,oio:source,S),
                                      curie_ns(S,Prefix),
                                      \+ ((rdf(A,oio:source,S2),
                                           S2\=S))),
                     [child_xrefs=X]-mondo_equiv_class_via_xref(C,X),
                     [parent_xrefs=X]-mondo_equiv_class_via_xref(D,X)
                    ],
                    [
                     description('is-a relationships with a single source'),
                     entity(child),
                     entity(parent),
                     iri(child_xrefs),
                     iri(parent_xrefs)
                     ]).
        

non_leaf_omim(X,Y,C,CY,Subsets) :-
        mondo_equiv_xref(X,Y,"OMIM"),
        subClassOf(C,X),
        mondo_equiv_xref(C,CY,"OMIM"),
        findall(S,in_subset(C,S),Subsets).

steal_isa(C,P,CX,PX) :-
        owl_equivalent_class_asserted_symm(C,CX),
        rdf(CX,rdfs:subClassOf,PX),
        owl_equivalent_class_asserted_symm(P,PX),
        \+ deprecated(P).


        

ordo_group(X) :-
        rdf(X,oio:inSubset,'http://purl.obolibrary.org/obo/mondo#ordo_group_of_disorders').

ordo_group_nox(C):-
        ordo_group(C),
        \+ ((xref_prefix(C,_,S),
             S\="Orphanet",
             S\="ICD10",
             S\="UMLS")).

ordo_group_nox_upper(C) :-
        ordo_group_nox(C),
        rdfs_subclass_of(C,D),
        mondo_equiv_xref(D,_DX,S),
        (   S="OMIMPS"
        ;   
            S="OMIM").

ordo_group_noxp(C):-
        ordo_group_nox(C),
        \+ \+ ((owl:subClassOf(D,D),
                ordo_group_nox(D))).

syndrome_and_neoplasm(C,N1) :-
        enlabel_of("neoplasm (disease)",N),
        enlabel_of("syndromic disease",S),
        rdfs_subclass_of(C,S),
        rdfs_subclass_of(C,N1),
        owl:subClassOf(N1,N).

mqtest(foo).


        
        
