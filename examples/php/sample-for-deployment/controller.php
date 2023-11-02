<?php
namespace Concrete\Package\SampleComposerPackage;

use Concrete\Core\Http\ServerInterface;
use Concrete\Core\Package\Package;
use Custom\Space\Middleware;

class Controller extends Package
{

    protected $appVersionRequired = '8.1.0';
    protected $pkgVersion = '0.1';
    protected $pkgHandle = 'sample_composer_package';
    protected $pkgName = 'Sample Composer Package';
    protected $pkgDescription = 'A concrete5 package powered by composer';

    public function on_start()
    {
        // Extend the ServerInterface binding so that when concrete5 creates the http server we can add our middleware
        $this->app->extend(ServerInterface::class, function(ServerInterface $server) {
            // Add our custom middleware
            return $server->addMiddleware($this->app->make(Middleware::class));
        });
    }

}
