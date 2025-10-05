-- Initialize H2GIS
CREATE ALIAS IF NOT EXISTS H2GIS FOR "org.h2gis.functions.factory.H2GISFunctions.load";
CALL H2GIS();
