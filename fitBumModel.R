#!/usr/bin/env Rscript

#
# Fit a beta-uniform mixture model to a list of p-values.
#
# Run with the -h flag for more information.
#
# Requires R (http://www.r-project.org/) to be installed. 
#


##
## BioNet functions, see the package documentation for more information
##

#
# *** beta uniform mixture model with shape2 fixed @1
#
fbum <- function(x, lambda, a){ lambda+(1-lambda)*a*x^(a-1) };

#
#     Log likelihood of BUM model
# ... version for optim
fbumLL  <- function(parms, x){sum(log(fbum(x, parms[1], parms[2])))};

# ... standard optim
bumOptim <- function(x, starts=1, labels=NULL)
{
  if(is.null(names(x)) && is.null(labels))
  {
    warning("Please name the p-values with the gene names or give labels!")
    names(x) <- as.character(1:length(x))
  }
  if(!is.null(labels))
  {
        names(x) <- labels
  }
  a <- runif(starts, 0.3, 0.7)
  lambda <- runif(starts, 0.3, 0.7)
  value <- Inf
  best <- list()
  for(i in 1:starts)
  {
    test.optim <- try(opt <- optim(c(lambda[i], a[i]), fn=.fbumnLL, gr=.fpLL, x=x, lower=rep(1e-5,3), method="L-BFGS-B", upper=rep(1-1e-5,3)))
    if ((!class(test.optim)=="try-error") && all(opt$par >= 1e-5) && all(opt$par <= 1-1e-5))
    {
      value <- opt$value
      best <- opt
    }
  }
  if(length(best)==0)
  {
    return(warning("BUM model could not be fitted to data"))
  }
  else
  {
    if (any(opt$par == 1e-5) || any(opt$par == 1-1e-5))
    {
      warning("One or both parameters are on the limit of the defined parameter space")
    }
    ret <- list(lambda=best$par[1], a=best$par[2], negLL=best$value, pvalues=x)
    class(ret) <- "bum"
    return(ret)
  }
}

# print function
# adapted to accept a filename or connection as the argument
print.bum <- function(x, file = "")
{
  cat("Beta-Uniform-Mixture (BUM) model\n\n", file = file, append = FALSE);
  cat(paste(length(x$pvalues), "pvalues fitted\n\n"), file = file, append = TRUE);
  cat(sprintf("Mixture parameter (lambda):\t%1.3f\n", x$lambda), file = file, append = TRUE);
  cat(sprintf("shape parameter (a): \t\t%1.3f\n", x$a), file = file, append = TRUE);
  cat(sprintf("log-likelihood:\t\t\t%.1f\n", -x$negLL), file = file, append = TRUE);
}

# fit bum model to p-values
# fit.fb(p.values, plot=TRUE)
# arguments:
#   p.values: p-values
#   plot: whether to plot a qqplot and a histogram of the fitted values
# values: fitted model
fitBumModel <- function (x, plot = TRUE, starts=10)
{
    if (is.null(names(x)))
    {
        #warning("Please name the p-values with the gene names!")
        names(x) = as.character(1:length(x))
    }
    fit <- bumOptim(x = x, starts)
    if (plot)
    {
        par(mfrow = c(1, 2))
        hist(x = fit)
        plot(fit)
    }
    return(fit)
}

#
# *** upper bound pi for the fraction of noise
#     (see Pounds an Morris)
#
piUpper <- function(fb) { return(fb$lambda + (1 - fb$lambda)*fb$a) }

#
#
#
# ... gradient of .fLL
.fpLL  <-function(parms, x)
{
 l <- parms[1]; a <- parms[2];

 dl <- -sum((1-a*x^(a-1))/(a*(1-l)*x^(a-1)+l));

 da <- -sum((a*(1-l)*x^(a-1)*log(x)+(1-l)*x^(a-1))/(a*(1-l)*x^(a-1)+l));

 return(c(dl,da));
}

# negative log likelihood
.fbumnLL <- function(parms, x){-fbumLL(parms, x)}

# qqplot.bum(pvalues, fb)
# arguments:
#   pvalues: vector of p-values
#   fb: fitted bum model to the p-value distribution
# values: plot -> quantiles of the bum distribution and the observed p-values
plot.bum <- function(x, main="QQ-Plot", xlab="Estimated p-value", ylab="Observed p-value", ...)
{
        n <- length(x$pvalues)
        probs <- (rank(sort(x$pvalues))-.5)/n
        # get quantiles of the bum distribution
        quantiles <- unlist(sapply(probs, uniroot, f=.pbum.solve, interval=c(0,1), lambda=x$lambda, a=x$a)[1,])
        plot(c(0,1),c(0,1), main=main, xlab=xlab, ylab=ylab, type="n", ...)
        lines(quantiles, sort(x$pvalues), lty=2)
        lines(c(0,1),c(0,1), col="grey")
}

# hist.bum(pvalues, fb)
# arguments:
#   pvalues: vector of p-values
#   fb: fitted bum model to the p-value distribution
# values: plot -> histogram of p-values with fitted bum distribution
hist.bum <- function(x, breaks=50, main="Histogram of p-values", xlab="P-values", ylab="Density", ...)
{
        hist(x$pvalues, breaks=breaks,  probability=TRUE, main=main, xlab=xlab, ylab=ylab, ...)
        bum.data <- seq(from=0, to=1, 1/100)
        lines(bum.data, x$lambda+(1-x$lambda)*x$a*bum.data^(x$a-1), lwd=3, col="red3");
        abline(h=piUpper(x), col="blue3", lwd=2);
        axis(side=2, labels=expression(pi), at=piUpper(x));
}

# internal function for root detection
.pbum.solve <- function(x, lambda ,a, proba)
{
  return((lambda*x+(1-lambda)*x^a)-proba)
}



##
## Non-BioNet functions
##

#
# Execute as a script.
#
# Run with the -h flag for more information.
#
main <- function(argv) {
    suppressPackageStartupMessages(
        if (!require(optparse, quietly = TRUE)) {
           cat("Could not load required R package `optparse'. Please run",
               "`install.packages(\"optparse\")' from within R to install it.",
               "",
               sep="\n") 
        quit(status = 1)
        })

    optionList <- list(
        make_option(
            c("-i", "--input-file"),
            type = "character",
            default = "-",
            metavar = "filename",
            help = "Filename of list of p-values to fit to, one per line [default: `-' for stdin]"),
        make_option(
            c("-O", "--output-file"),
            type = "character",
            default = "-",
            metavar = "filename",
            help = "Filename to write output to [default: `-' for stdout]"),
        make_option(
            c("-p", "--plot-file"),
            type = "character",
            default = "-",
            metavar = "filename",
            help = "PNG file to write fit evaluation plots to [default: `-' for no plots]"),
        make_option(
            c("-s", "--starts"),
            type = "integer",
            default = 10,
            metavar = "integer",
            help = "Number of starting points for the optimisation [default: %default]"))
    opt <- parse_args(
        OptionParser(
            option_list = optionList,
            description = "Fit a BUM model to a list of p-values, estimating its parameters.",
            epilogue = paste("Based on functions extracted from the BioConductor package `BioNet',",
                             "version 1.24.0. See http://www.bioconductor.org/packages/2.14/bioc/html/BioNet.html",
                             sep = "\n")),
        argv)

    # if the input file parameter is -
    inputFile <- if (opt$`input-file` ==  "-") {
        # use the (C-level) standard input stream
        file("stdin")
    } else {
        # open the file for reading, not assuming it’s seekable
        # (otherwise it will fail confusingly for named pipes)
        file(opt$`input-file`, open = "r", raw = TRUE)
    }

    # read the lines from the input file as character strings
    pValues <- read.table(inputFile, colClasses = c("numeric"))
    if (ncol(pValues) != 1) {
        stop("p-value file is not a single column.")
    }
    pValues <- pValues[[1]]
    if (any((is.na(pValues) | pValues < 0 | pValues > 1))) {
        stop(paste("Invalid value on line",
                   which(is.na(pValues) | pValues < 0 | pValues > 1)[1],
                   "of the p-value file."))
    }

    # open a graphics device if applicable
    if (opt$`plot-file` != "-") {
        # open a PNG-writing graphics device
        png(filename = opt$`plot-file`)
    }

    bumFit <- fitBumModel(
        pValues,
        plot = opt$`plot-file` != "-",
        starts = opt$`starts`)

    # write out and close the plot file if applicable
    if (opt$`plot-file` != "-") {
        dev.off()
    }
    
    # if the output file parameter is -
    outputFile <- if (opt$`output-file` ==  "-") {
        stdout()
    } else {
        file(opt$`output-file`, "w")
    }

    print(bumFit, file = outputFile)

    closeAllConnections()
}

main(commandArgs(trailingOnly = TRUE))
