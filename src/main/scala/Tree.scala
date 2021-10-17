sealed trait Tree[+A]
case class Node[+A](value: A, children: List[Node[A]]) extends Tree[A]

object Tree {
  def test(tree: Node[Int]) = {
    tree.children.foreach(c => println(c))
  }

  val example = Node(1, List(Node(2, List.empty), Node(3, List.empty)))
}