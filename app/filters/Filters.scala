package filters

import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
import play.filters.gzip.GzipFilter

import javax.inject.Inject

class Filters @Inject() (
    gzip: GzipFilter,
    log: LoggingFilter,
    corsFilter: CORSFilter
) extends DefaultHttpFilters(gzip, log, corsFilter)
