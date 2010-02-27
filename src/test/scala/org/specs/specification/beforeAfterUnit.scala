/**
 * Copyright (c) 2007-2010 Eric Torreborre <etorreborre@yahoo.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software. Neither the name of specs nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.specs.specification
import org.specs.specification
import org.specs.io.mock._
import org.specs.Sugar._

class beforeAfterUnit extends SpecificationWithJUnit {
  "A specification with 2 expectations in the doBefore clause must fail all examples if the expectations are wrong" in {
    object s extends Specification with MockOutput {
      doBeforeSpec { 1 must_== 2; 1 must_== 3 }
      "sys1" should {
        "have one ex ok" in { 1 must_== 1 }
        "have one ex ok" in { 1 must_== 1 }
      }
      "sys2" should {
        "have one ex ok" in { 1 must_== 1 }
        "have one ex ok" in { 1 must_== 1 }
      }
    }
    s.reportSpecs
    s.failures.size aka s.messages.mkString("\n") must_== 4
    s.failures(0).toString must include("Before specification") and include("not equal to '2'")
  }
  "A specification with 2 expectations in the doFirst clause must fail all examples if the expectations are wrong" in {
    object s extends Specification with MockOutput {
      "sys1" should {
        doFirst { 1 must_== 2; 1 must_== 3 }
        "have one ex ok-1" in { 1 must_== 1 }
        "have one ex ok-2" in { 1 must_== 1 }
      }
    }
    s.reportSpecs
    s.failures.size aka "the number of system failures" must_== 2
    s.failures(0).toString must include("Before system") and include("not equal to '2'")
  }
  "A specification with 1 expectation in the afterSpec clause must not throw an exception - see issue 122" in {
    s1.reportSpecs
    s1.failures.pp.size aka "the number of system failures" must_== 1
    s1.failures(0).toString must include("After system") and include("not equal to '2'")
  }
}
    object s1 extends Specification {//with MockOutput {
      doAfterSpec { 
        1 must_== 2
      }
      "sys1" should {
        "have one ex ok-1" in { 1 must_== 1 }
        "have one ex ok-2" in { 1 must_== 2 }
      }
    }
