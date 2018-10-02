var nodeList = document.body.getElementsByTagName("code");
nodes = Array.prototype.slice.call(nodeList, 0);

nodes.forEach(function (node) {
  var text = node.innerHTML
  node.innerHTML = text.replace(/<br\s*>/g, '\n')
});

CodeMirror.colorize(nodeList, "${default_mode}")