<?php

namespace Custom\Space;

use Concrete\Core\Config\Repository\Repository;
use Concrete\Core\Http\Middleware\DelegateInterface;
use Concrete\Core\Http\Middleware\MiddlewareInterface;
use Symfony\Component\HttpFoundation\Request;

/**
 * Custom middleware that adds information to the response
 */
class Middleware implements MiddlewareInterface
{

    /** @var \Concrete\Core\Config\Repository\Repository */
    protected $config;

    public function __construct(Repository $config)
    {
        $this->config = $config;
    }

    /**
     * Process the request and return a response
     * @param \Symfony\Component\HttpFoundation\Request $request
     * @param DelegateInterface $frame
     * @return \Symfony\Component\HttpFoundation\Response
     */
    public function process(Request $request, DelegateInterface $frame)
    {
        // Get the response object from the next middleware
        $response = $frame->next($request);

        // Set the custom headers
        $response->headers->add([
            'x-concrete5-version' => $this->config->get('concrete.version')
        ]);

        // Return the modified response to the previous middleware
        return $response;
    }
}
