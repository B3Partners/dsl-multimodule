# datastorelinker-bom
Bill of Materials voor datastorelinker






## bouwen vanaf broncode

checkout de volgende repositories:

```

git clone git@github.com:B3Partners/datastorelinker-bom.git
git clone git@github.com:B3Partners/jump-b3p.git
git clone git@github.com:B3Partners/securityfilter-b3p.git
git clone git@github.com:B3Partners/commons-mail-b3p.git
git clone git@github.com:B3Partners/b3p-commons-core.git
git clone git@github.com:B3Partners/b3p-commons-gis.git
git clone git@github.com:B3Partners/b3p-mapfile.git
git clone git@github.com:B3Partners/b3p-suf2-reader.git
git clone git@github.com:B3Partners/b3p-gt2-suf2.git
git clone git@github.com:B3Partners/b3p-gt2-csv.git
git clone git@github.com:B3Partners/b3p-gt2-dxf.git
git clone git@github.com:B3Partners/b3p-gt2-msaccess.git
git clone git@github.com:B3Partners/b3p-gt2-ogr.git
git clone git@github.com:B3Partners/b3p-gt2-sdl.git
git clone git@github.com:B3Partners/datastorelinkerEntities.git
git clone git@github.com:B3Partners/b3p-datastorelinker.git
git clone git@github.com:B3Partners/datastorelinker.git


```
Doe vervolgens een build+ deploy van de `datastorelinker-bom`:

```
cd datastorelinker-bom
mvn clean install deploy

```

Hierna kunnen de afzonderlijke artefacten worden gebouwd (met `mvn clean install`) van de juiste branch.

```

```

