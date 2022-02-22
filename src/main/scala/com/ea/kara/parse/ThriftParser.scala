/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara.parse

import java.io.File

import com.twitter.scrooge.ast.Document
import com.twitter.scrooge.frontend.{Importer, MultiImporter, ThriftParser => ScroogeThriftParser}

class ThriftParser(sourceDirectories: Seq[File]) {
  private def allSubDirectories(file: File): Seq[File] =
    if (file.isDirectory) file.listFiles().flatMap(allSubDirectories).toSeq :+ file else Nil
  private val importer = MultiImporter(
    sourceDirectories.flatMap(allSubDirectories).map(Importer(_))
  )
  private val parser                    = new ScroogeThriftParser(importer = importer, strict = true)
  def parse(fileName: String): Document = parser.parseFile(fileName)
}
