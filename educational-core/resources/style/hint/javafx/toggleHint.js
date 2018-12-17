$(window).load(function () {

  $(".hint_header").click(function () {
    $header = $(this);
    $content = $header.next();

    $content.slideToggle(200, function () {
      // hack to change arrow icon by adding/removing fake class. See style/hint/base.css
      $header.toggleClass('checked');
    });
  });
});
